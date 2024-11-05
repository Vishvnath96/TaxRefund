package com.interview.taxrefund.domain.syncmanager

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.interview.taxrefund.core.config.PredictionConfig
import com.interview.taxrefund.core.config.RefreshConfig
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

@Singleton
class SyncManager @Inject constructor(
    private val workManager: WorkManager,
    private val refreshConfig: RefreshConfig,
    private val requestTracker: RequestTracker
) {
    fun scheduleRefresh(refundId: String) {
        val constraints = createTrafficAwareConstraints()
        val interval = calculateSyncInterval()

        val request = PeriodicWorkRequestBuilder<RefundSyncWorker>(interval)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Duration.minutes(10)
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "refund_sync_$refundId",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    private fun calculateSyncInterval(): Duration {
        return when (getCurrentTrafficPattern()) {
            TrafficPattern.PEAK_HIGH_TRAFFIC ->
                refreshConfig.refreshIntervals.highTrafficInterval

            TrafficPattern.PEAK_NORMAL_TRAFFIC ->
                refreshConfig.refreshIntervals.peakSeasonInterval

            TrafficPattern.PEAK_OFF_HOURS ->
                refreshConfig.refreshIntervals.normalTrafficInterval

            TrafficPattern.OFF_PEAK ->
                refreshConfig.refreshIntervals.offPeakInterval
        }
    }

    /**
     * Determines current traffic pattern based on:
     * 1. Tax season dates
     * 2. Time of day
     * 3. Current request rate
     */
    private fun getCurrentTrafficPattern(): TrafficPattern {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        return when {
            // First check if we're in tax season
            isInTaxSeason(currentDate.date) -> {
                // Then check if we're in peak hours
                if (isInPeakHours(currentDate.time)) {
                    // Finally check current request rate
                    if (isHighTraffic()) {
                        TrafficPattern.PEAK_HIGH_TRAFFIC
                    } else {
                        TrafficPattern.PEAK_NORMAL_TRAFFIC
                    }
                } else {
                    TrafficPattern.PEAK_OFF_HOURS
                }
            }
            // Off season pattern
            else -> TrafficPattern.OFF_PEAK
        }
    }

    private fun isInTaxSeason(currentDate: LocalDate): Boolean {
        return currentDate in refreshConfig.taxSeasonConfig.peakSeasonStart
                refreshConfig.taxSeasonConfig.peakSeasonEnd
    }

    private fun isInPeakHours(currentTime: LocalTime): Boolean {
        return currentTime in refreshConfig.taxSeasonConfig.peakHours.start
                refreshConfig.taxSeasonConfig.peakHours.end
    }

    private fun isHighTraffic(): Boolean {
        val recentRequests = requestTracker.getRequestCountInLastMinute()
        return recentRequests >= refreshConfig.taxSeasonConfig.highTrafficThreshold
    }



    suspend fun cancelRefresh(refundId: String) {
        workManager.cancelUniqueWork("refund_sync_$refundId")
    }

    suspend fun cancelAllRefresh() {
        workManager.cancelAllWork()
    }
}


enum class TrafficPattern {
    PEAK_HIGH_TRAFFIC,
    PEAK_NORMAL_TRAFFIC,
    PEAK_OFF_HOURS,
    OFF_PEAK
}