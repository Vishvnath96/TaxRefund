package com.interview.taxrefund.core.di

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.interview.taxrefund.core.security.encryption.EncryptionManager
import com.interview.taxrefund.data.cache.RefundCache
import com.interview.taxrefund.data.cache.RefundCacheImpl
import com.interview.taxrefund.data.cache.config.CacheConfiguration
import com.interview.taxrefund.data.local.db.dao.RefundDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import java.time.Duration
import javax.inject.Qualifier
import javax.inject.Singleton


//High traffic cache config
@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @Provides
    @Singleton
    fun provideRefundCache(
        dao: RefundDao,
        encryptionManager: EncryptionManager,
        dispatchers: CoroutineDispatcher
    ): RefundCache = RefundCacheImpl(dao, encryptionManager, dispatchers)

    @Provides
    @CacheConfig
    fun provideCacheConfig(): CacheConfiguration = CacheConfiguration(
        maxSize = 1000,
        expireAfterWrite = Duration.ofMinutes(15),
        expireAfterAccess = Duration.ofMinutes(30)
    )


    @Provides
    @Singleton
    fun provideMemoryCache(): Cache<String, ByteArray> {
        return Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofMinutes(15))
            .build()
    }
}










@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CacheConfig