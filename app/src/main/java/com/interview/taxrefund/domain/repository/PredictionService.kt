package com.interview.taxrefund.domain.repository

import com.interview.taxrefund.core.common.Results
import com.interview.taxrefund.domain.model.prediction.RefundPrediction

interface PredictionService {
    /**
     * Get a single prediction for refund availability
     * @param refundId Current refund ID
     * @return Result containing the prediction or error
     */
    suspend fun getPrediction(refundId: String): Results<RefundPrediction>

    /**
     * Update prediction accuracy with actual results
     * @param refundId Refund ID
     * @param actualProcessingDays Actual days taken
     */
    suspend fun updatePredictionAccuracy(refundId: String, actualProcessingDays: Int)
}