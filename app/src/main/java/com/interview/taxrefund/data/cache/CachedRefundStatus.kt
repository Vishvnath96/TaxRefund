package com.interview.taxrefund.data.cache

import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import com.interview.taxrefund.domain.model.refund.RefundStatus
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class CachedRefundStatus(
    val status: RefundStatus,
    val timestamp: Instant,
    val isValid: Boolean
) {
    fun isStale(validityDuration: Duration): Boolean =
        Duration.between(timestamp, Instant.now()) > validityDuration
}

data class CachedPrediction(
    val prediction: RefundPrediction,
    val timestamp: Instant,
    val isStale: Boolean
)