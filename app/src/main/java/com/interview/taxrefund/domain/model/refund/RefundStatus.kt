package com.interview.taxrefund.domain.model.refund

import com.interview.taxrefund.core.metrics.UserGuidance
import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

data class RefundStatus(
    val id: String, //Unique identifier for the refund request

    val status: RefundStatusType,// Current status of refund (PROCESSING, DELAYED, AVAILABLE, etc.)

    val amount: BigDecimal?, // Refund amount (nullable)
    val filingDate: LocalDate, // Date when tax return was filed

    val lastUpdated: Instant,  // Timestamp of last status update

    val prediction: RefundPrediction?,  // Prediction data if refund isn't available yet

    val issues: List<RefundIssue> = emptyList(),  // List of issues/problems with the refund

    val guidance: List<UserGuidance> = emptyList() // User-friendly guidance based on status/issues

    // Load-related information for traffic handling
    val loadInfo: LoadInfo,

    // Data freshness information

) {
    val needsPrediction: Boolean
        get() = status == RefundStatusType.PROCESSING && prediction == null
    val isStale: Boolean
        get() = Duration.between(lastUpdated, Instant.now()) > Duration.ofHours(1)
}


// Supporting Models
data class LoadInfo(
    val refreshInterval: Duration,  // When to refresh next
    val backoffPeriod: Duration?,  // Optional wait period
    val timestamp: Instant         // When info was received
)

enum class RefundStatusType {
    PROCESSING,
    DELAYED,
    AVAILABLE,
    ERROR
}


