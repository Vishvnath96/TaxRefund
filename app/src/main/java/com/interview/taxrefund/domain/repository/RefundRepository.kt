package com.interview.taxrefund.domain.repository

import com.interview.taxrefund.core.common.Results
import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import com.interview.taxrefund.domain.model.refund.RefundStatus
import kotlinx.coroutines.flow.Flow

//Repository Interfaces
interface RefundRepository {
    /**
     * Get most recent tax refund status with prediction if needed
     * Handles:
     * - High traffic with caching
     * - IRS delays with fallback mechanisms
     * - Security with encryption
     */
    suspend fun getMostRecentRefundStatus(): Flow<Results<RefundStatus>>

    /**
     * Force refresh status from IRS
     * Use when cached data is stale
     */
    suspend fun refreshRefundStatus(): Flow<Results<RefundStatus>>

    /**
     * Get AI prediction for refund timing
     * Used when refund is not yet available
     */
    suspend fun getPrediction(refundId: String): Flow<Results<RefundPrediction>>

    /**
     * Clear sensitive data
     * Security requirement for data privacy
     */
    suspend fun clearData()
}