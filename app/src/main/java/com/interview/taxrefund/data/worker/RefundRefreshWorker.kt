package com.interview.taxrefund.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.interview.taxrefund.core.analytics.AnalyticsTracker
import com.interview.taxrefund.core.common.extension.isRetryable
import com.interview.taxrefund.domain.repository.RefundRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class RefundRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: RefundRepository,
    private val analyticsTracker: AnalyticsTracker
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val refundId = inputData.getString(KEY_REFUND_ID)
            ?: return Result.failure()

        return try {
            repository.refreshRefundStatus()
                .first()
                .fold(
                    onSuccess = {
                        analyticsTracker.trackRefreshSuccess(refundId)
                        Result.success()
                    },
                    onError = { error ->
                        analyticsTracker.trackRefreshError(refundId, error)
                        if (error.isRetryable() && runAttemptCount < MAX_RETRIES) {
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    }
                )
        } catch (e: Exception) {
            analyticsTracker.trackRefreshError(refundId, e)
            if (e.isRetryable() && runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_REFUND_ID = "refund_id"
        private const val MAX_RETRIES = 3
    }
}
