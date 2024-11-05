package com.interview.taxrefund.data.repository.prediction

import android.util.Log
import com.interview.taxrefund.core.common.Results
import com.interview.taxrefund.core.config.PredictionConfig
import com.interview.taxrefund.data.cache.predictioncache.PredictionCache
import com.interview.taxrefund.data.remote.api.ai.AIApi
import com.interview.taxrefund.domain.model.prediction.DelayFactor
import com.interview.taxrefund.domain.model.prediction.DelayType
import com.interview.taxrefund.domain.model.prediction.ImpactLevel
import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import com.interview.taxrefund.domain.repository.PredictionService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToInt

@Singleton
class AIRefundPredictionService @Inject constructor(
    private val aiApi: AIApi,
    private val cache: PredictionCache,
    private val config: PredictionConfig,
    private val dispatchers: CoroutineDispatcher
) : PredictionService {

    override suspend fun getPrediction(refundId: String): Results<RefundPrediction> =
        withContext(dispatchers) {
            try {
                // Try cache first
                cache.get(refundId)?.let { cached ->
                    if (!cached.isStale) {
                        return@withContext Results.Success(cached.prediction)
                    }
                }

                // Get fresh prediction
                val prediction = aiApi.getPrediction(refundId)
                    .toDomain()
                    .validate(config)

                // Cache result
                cache.save(refundId, prediction)

                Results.Success(prediction)
            } catch (e: Exception) {
                createFallbackPrediction().fold(
                    onSuccess = { Results.Success(it) },
                    onError = { Results.Error(e) }
                )
            }
        }

    override suspend fun updatePredictionAccuracy(
        refundId: String,
        actualProcessingDays: Int
    ) {
        withContext(dispatchers) {
            try {
                cache.get(refundId)?.prediction?.let { prediction ->
                    if (shouldAdjustModel(
                            predicted = prediction.estimatedDays,
                            actual = actualProcessingDays
                        )) {
                        aiApi.updateModel(
                            refundId = refundId,
                            predictedDays = prediction.estimatedDays,
                            actualDays = actualProcessingDays
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update prediction accuracy", e)
            }
        }
    }

    // Use config values instead of constants
    private fun createFallbackPrediction(): Results<RefundPrediction> {
        return try {
            Results.Success(
                RefundPrediction(
                    estimatedDays = config.processing.defaultProcessingDays,
                    confidence = config.confidence.fallback,
                    estimatedDate = LocalDate.now().plusDays(
                        config.processing.defaultProcessingDays.toLong()
                    ),
                    factors = listOf(
                        DelayFactor(
                            type = DelayType.SYSTEM_DELAY,
                            impact = ImpactLevel.LOW,
                            description = "Using standard estimate"
                        )
                    )
                )
            )
        } catch (e: Exception) {
            Results.Error(e)
        }
    }

    private fun shouldAdjustModel(predicted: Int, actual: Int): Boolean {
        val accuracy = 1 - abs(predicted - actual) / predicted.toFloat()
        return accuracy < config.confidence.minimum
    }

    companion object {
        private const val TAG = "AIRefundPrediction"
    }
}
