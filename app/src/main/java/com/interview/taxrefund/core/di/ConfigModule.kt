package com.interview.taxrefund.core.di

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.interview.taxrefund.core.config.PredictionConfig
import com.interview.taxrefund.core.config.RefreshConfig
import com.interview.taxrefund.core.config.RefreshIntervals
import com.interview.taxrefund.core.config.RemoteConfig
import com.interview.taxrefund.core.config.RetryPolicy
import com.interview.taxrefund.core.config.TaxSeasonConfig
import com.interview.taxrefund.data.cache.CachePolicy
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideRefreshConfig(
        @ApplicationContext context: Context
    ): RefreshConfig {
        return try {
            // Try to load from remote config first
            RemoteConfig.getRefreshConfig()
        } catch (e: Exception) {
            // Fallback to local config
            context.assets.open("config/refresh_config.json").use { input ->
                val json = input.bufferedReader().readText()
                Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                    .adapter(RefreshConfig::class.java)
                    .fromJson(json)
            } ?: createDefaultConfig()
        }
    }

    @Provides
    @Singleton
    fun providePredictionConfig(
        @ApplicationContext context: Context,
        remoteConfig: FirebaseRemoteConfig
    ): PredictionConfig {
        return try {
            // Try to get from Firebase Remote Config first
            val configJson = remoteConfig.getString(KEY_PREDICTION_CONFIG)
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(PredictionConfig::class.java)
                .fromJson(configJson)
                ?: getLocalConfig(context)
        } catch (e: Exception) {
            getLocalConfig(context)
        }
    }

    private fun getLocalConfig(context: Context): PredictionConfig {
        return try {
            // Read from local config file
            context.assets.open("config/prediction_config.json.json").use { input ->
                val json = input.bufferedReader().readText()
                Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                    .adapter(PredictionConfig::class.java)
                    .fromJson(json)
            } ?: PredictionConfig() // Use defaults if file read fails
        } catch (e: Exception) {
            PredictionConfig() // Use defaults if all else fails
        }
    }

    private fun createDefaultConfig() = RefreshConfig(
        taxSeasonConfig = TaxSeasonConfig(),
        refreshIntervals = RefreshIntervals(),
        retryPolicy = RetryPolicy(),
        cachePolicy = CachePolicy()
    )

    private const val KEY_PREDICTION_CONFIG = "prediction_config.json"

}