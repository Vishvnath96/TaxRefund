package com.interview.taxrefund.data.cache.predictioncache

import com.github.benmanes.caffeine.cache.Cache
import com.interview.taxrefund.data.cache.CachedPrediction
import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface PredictionCache {
    suspend fun get(refundId: String): CachedPrediction?
    suspend fun save(refundId: String, prediction: RefundPrediction)
    suspend fun clear()
}

@Singleton
class PredictionCacheImpl @Inject constructor(
    private val cache: Cache<String, CachedPrediction>
) : PredictionCache {
    override suspend fun get(refundId: String): CachedPrediction? =
        cache.getIfPresent(refundId)

    override suspend fun save(refundId: String, prediction: RefundPrediction) {
        val cached = CachedPrediction(
            prediction = prediction,
            timestamp = Clock.System.now(),
            isStale = false
        )
        cache.put(refundId, cached)
    }

    override suspend fun clear() = cache.invalidateAll()
}