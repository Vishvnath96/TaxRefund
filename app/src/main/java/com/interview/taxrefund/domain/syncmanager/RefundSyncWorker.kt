package com.interview.taxrefund.domain.syncmanager

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.interview.taxrefund.domain.repository.RefundRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first


@HiltWorker
class RefundSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: RefundRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val refundId = inputData.getString(KEY_REFUND_ID) ?: return Result.failure()

        return try {
            repository.refreshRefundStatus()
                .first() // Take first emission
                .fold(
                    onSuccess = { Result.success() },
                    onError = {
                        if (runAttemptCount < MAX_RETRIES) {
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    }
                )
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val MAX_RETRIES = 3
        const val KEY_REFUND_ID = "refund_id"
    }
}