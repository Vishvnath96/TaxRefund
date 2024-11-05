package com.interview.taxrefund.domain.usecase.prediction

import com.interview.taxrefund.core.common.Results
import com.interview.taxrefund.core.config.PredictionConfig
import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import com.interview.taxrefund.domain.repository.RefundRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject



class GetPredictionUseCase @Inject constructor(
    private val repository: RefundRepository,
    private val config: PredictionConfig,
    private val dispatchers: CoroutineDispatcher
) {
    suspend operator fun invoke(refundId: String): Flow<Results<RefundPrediction>> =
        repository.getPrediction(refundId)
            .map { result ->
                when (result) {
                    is Results.Success -> validatePrediction(result.data)
                    is Results.Error -> result
                }
            }
            .flowOn(dispatchers)

    private fun validatePrediction(prediction: RefundPrediction): Results<RefundPrediction> {
        return when {
            !isProcessingDaysValid(prediction.estimatedDays) -> {
                Results.Error(
                    InvalidPredictionException(
                        "Processing days ${prediction.estimatedDays} outside valid range " +
                                "[${config.processing.minProcessingDays}..${config.processing.maxProcessingDays}]"
                    )
                )
            }
            !isConfidenceValid(prediction.confidence) -> {
                Results.Error(
                    LowConfidencePredictionException(
                        "Confidence ${prediction.confidence} below minimum threshold " +
                                "${config.confidence.minimum}"
                    )
                )
            }
            else -> Results.Success(prediction)
        }
    }

    private fun isProcessingDaysValid(days: Int): Boolean {
        return days in config.processing.minProcessingDays..config.processing.maxProcessingDays
    }

    private fun isConfidenceValid(confidence: Float): Boolean {
        return confidence >= config.confidence.minimum
    }
}

// Custom Exceptions for better error handling
sealed class PredictionException(message: String) : Exception(message)
class InvalidPredictionException(message: String) : PredictionException(message)
class LowConfidencePredictionException(message: String) : PredictionException(message)
