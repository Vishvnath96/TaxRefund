package com.interview.taxrefund.core.analytics

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.interview.taxrefund.core.analytics.db.AnalyticsDatabase
import com.interview.taxrefund.core.analytics.worker.MetricsSyncWorker
import com.interview.taxrefund.core.metrics.ApiMetrics
import com.interview.taxrefund.core.metrics.ErrorMetrics
import com.interview.taxrefund.core.metrics.UserAction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsTrackerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsDatabase: AnalyticsDatabase,
    private val workManager: WorkManager,
    private val scope: CoroutineScope
) : AnalyticsTracker {

    override fun trackApiCall(metrics: ApiMetrics) {
        scope.launch(Dispatchers.IO) {
            // Store locally first
            analyticsDatabase.apiMetricsDao().insert(metrics.toEntity())

            // Schedule background sync if needed
            if (shouldSyncMetrics()) {
                scheduleMetricsSync()
            }

            // Track performance issues
            if (metrics.duration > PERFORMANCE_THRESHOLD) {
               // reportPerformanceIssue(metrics)
            }
        }
    }

    override fun trackError(error: Throwable, metadata: Map<String, Any>?) {
        scope.launch(Dispatchers.IO) {
            val errorMetrics = ErrorMetrics(
                errorClass = error.javaClass.simpleName,
                message = error.message,
                stackTrace = error.stackTraceToString(),
                metadata = metadata,
                timestamp = System.currentTimeMillis()
            )
            analyticsDatabase.errorMetricsDao().insert(errorMetrics.toEntity())
        }
    }

    override fun trackUserAction(action: String, parameters: Map<String, Any>?) {
        scope.launch(Dispatchers.IO) {
            val userAction = UserAction(
                action = action,
                parameters = parameters,
                timestamp = System.currentTimeMillis()
            )
            analyticsDatabase.userActionDao().insert(userAction.toEntity())
        }
    }

    private fun shouldSyncMetrics(): Boolean {
        // Implement logic to decide when to sync
        // For example, based on number of records or time elapsed
        return analyticsDatabase.apiMetricsDao().count() >= SYNC_THRESHOLD
    }

    private fun scheduleMetricsSync() {
        val syncWork = OneTimeWorkRequestBuilder<MetricsSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncWork
        )
    }

    companion object {
        private const val PERFORMANCE_THRESHOLD = 3000L // 3 seconds
        private const val SYNC_THRESHOLD = 100 // records
        private const val SYNC_WORK_NAME = "metrics_sync_work"
    }
}
