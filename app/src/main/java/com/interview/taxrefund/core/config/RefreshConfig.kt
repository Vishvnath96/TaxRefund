package com.interview.taxrefund.core.config

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class RefreshConfig(
    val taxSeasonConfig: TaxSeasonConfig,
    val refreshIntervals: RefreshIntervals,
    val retryPolicy: RetryPolicy,
    val cachePolicy: CachePolicy
)

data class TaxSeasonConfig(
    val startMonth: Int = 1,  // January
    val endMonth: Int = 4,    // April
    val businessHours: BusinessHours = BusinessHours()
)

data class BusinessHours(
    val startHour: Int = 9,   // 9 AM
    val endHour: Int = 17     // 5 PM
)

data class RefreshIntervals(
    val taxSeason: TaxSeasonIntervals = TaxSeasonIntervals(),
    val offSeason: OffSeasonIntervals = OffSeasonIntervals()
)

data class TaxSeasonIntervals(
    val businessHours: Long = 30,    // 30 minutes
    val offHours: Long = 120         // 2 hours
)

data class OffSeasonIntervals(
    val regular: Long = 240          // 4 hours
)

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMinutes: Long = 1,
    val maxDelayMinutes: Long = 30,
    val backoffMultiplier: Double = 2.0
)

data class CachePolicy(
    val standardTtlMinutes: Long = 15,
    val extendedTtlMinutes: Long = 240
)


object RemoteConfig {
    private const val KEY_REFRESH_CONFIG = "refresh_config"

    fun getRefreshConfig(): RefreshConfig {
        // Implementation using Firebase Remote Config or similar
        // This allows dynamic updates to configuration
        return Firebase.remoteConfig
            .getString(KEY_REFRESH_CONFIG)
            .let { json ->
                Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                    .adapter(RefreshConfig::class.java)
                    .fromJson(json)
            } ?: createDefaultConfig()
    }
}