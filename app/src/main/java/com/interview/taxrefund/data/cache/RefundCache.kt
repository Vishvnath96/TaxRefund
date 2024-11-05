package com.interview.taxrefund.data.cache

import android.util.Log
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.interview.taxrefund.core.security.encryption.EncryptionManager
import com.interview.taxrefund.data.local.db.RefundDatabase
import com.interview.taxrefund.data.local.db.dao.RefundDao
import com.interview.taxrefund.data.local.entity.RefundStatusEntity
import com.interview.taxrefund.domain.cache.RefundCache
import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import com.interview.taxrefund.domain.model.refund.RefundStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.Cache
import retrofit2.http.Headers
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

//Caching Strategy
interface RefundCache {
    suspend fun getStatus(refundId: String): CachedRefundStatus?
    suspend fun saveStatus(refundId: String, status: RefundStatus)
    suspend fun invalidateStatus(refundId: String)
    suspend fun clear()
}

@Singleton
class SecureRefundCache @Inject constructor(
    private val database: RefundDatabase,
    private val encryption: EncryptionManager,
    private val config: RefundConfig
) : RefundCache {

    override suspend fun getStatus(refundId: String): CachedRefundStatus? {
        return database.refundDao().getStatus(refundId)
        .takeIf { !it.isExpired()}
        .let { entity ->
            try {
                CachedRefundStatus(
                    status = encryption.decrypt(entity.encryptedStatus),
                    timestamp = entity.timestamp,
                    isValid = !entity.isInvalidated
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun isExpired(headers: Headers, timestamp: Instant): Boolean {
        val maxAge = headers.getCacheControl()?.maxAge ?: DEFAULT_MAX_AGE
        return Clock.System.now() - timestamp > maxAge
    }

    override suspend fun saveStatus(refundId: String, status: RefundStatus) {
        val encrypted = encryption.encrypt(status)
        database.refundDao().insertStatus(
            RefundStatusEntity(
                refundId = refundId,
                encryptedStatus = encrypted,
                timestamp = Clock.System.now(),
                isInvalidated = false
            )
        )
    }

    override suspend fun invalidateStatus(refundId: String) {
        database.refundDao().invalidateStatus(refundId)
    }

    override suspend fun clear() {
        database.refundDao().clearAll()
    }
}
