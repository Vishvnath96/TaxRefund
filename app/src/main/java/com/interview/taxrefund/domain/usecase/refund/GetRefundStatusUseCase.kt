package com.interview.taxrefund.domain.usecase.refund

import com.interview.taxrefund.core.common.Results
import com.interview.taxrefund.core.config.PredictionConfig
import com.interview.taxrefund.domain.model.historical.ProcessingMetrics
import com.interview.taxrefund.domain.model.refund.RefundStatus
import com.interview.taxrefund.domain.repository.PredictionService
import com.interview.taxrefund.domain.repository.RefundRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration

class GetRefundStatusUseCase @Inject constructor(
    private val repository: RefundRepository,
    private val config: PredictionConfig,
    private val dispatchers: CoroutineDispatcher
) {
    suspend operator fun invoke(forceRefresh: Boolean): Flow<Results<RefundStatus>> =
        if (forceRefresh) {
            repository.refreshRefundStatus()
        } else {
            repository.getMostRecentRefundStatus()
        }.flowOn(dispatchers)
            .catch { e ->
                emit(Results.Error(e as Exception))
            }

    // Observe metrics from status updates
    suspend fun observeMetrics(): Flow<ProcessingMetrics> =
        repository.getMostRecentRefundStatus()
            .map { result ->
                when (result) {
                    is Results.Success -> createMetricsFromStatus(result.data)
                    is Results.Error -> createDefaultMetrics()
                }
            }
            .flowOn(dispatchers)

    private fun createMetricsFromStatus(status: RefundStatus): ProcessingMetrics {
        return ProcessingMetrics(
            averageProcessingDays = calculateProcessingDays(status),
            currentLoadPercentage = calculateLoadPercentage(status),
            estimatedWaitTime = calculateWaitTime(status)
        )
    }

    private fun createDefaultMetrics(): ProcessingMetrics {
        return ProcessingMetrics(
            averageProcessingDays = config.processing.defaultProcessingDays.toFloat(),
            currentLoadPercentage = 0f,
            estimatedWaitTime = Duration.minutes(
                config.processing.defaultProcessingDays.toLong()
            )
        )
    }
}