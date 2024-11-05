package com.interview.taxrefund.core.di

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.interview.taxrefund.data.repository.prediction.AIRefundPredictionService
import com.interview.taxrefund.data.remote.api.ai.AIApi
import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import com.interview.taxrefund.domain.repository.PredictionService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import java.time.Duration
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PredictionModule {

    @Provides
    @Singleton
    fun provideAIApi(retrofit: Retrofit): AIApi =
        retrofit.create(AIApi::class.java)

    @Provides
    @Singleton
    fun providePredictionCache(): Cache<String, RefundPrediction> =
        Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofHours(1))
            .build()

    @Provides
    @Singleton
    fun providePredictionService(
        impl: AIRefundPredictionService
    ): PredictionService = impl
}