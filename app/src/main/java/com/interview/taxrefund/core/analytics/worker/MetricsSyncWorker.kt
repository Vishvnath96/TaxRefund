package com.interview.taxrefund.core.analytics.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.interview.taxrefund.core.analytics.db.AnalyticsDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MetricsSyncWorker @Inject constructor(
    @ApplicationContext context: Context,
    workerParams: WorkerParameters,
    private val analyticsApi: AnalyticsApi,
    private val analyticsDatabase: AnalyticsDatabase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            // Get unsynchronized metrics
            val metrics = analyticsDatabase.apiMetricsDao().getUnsyncedMetrics()

            // Send to backend
            analyticsApi.sendMetrics(metrics)

            // Mark as synced
            analyticsDatabase.apiMetricsDao().markAsSynced(
                metrics.map { it.requestId }
            )

            return Result.success()
        } catch (e: Exception) {
            return if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}