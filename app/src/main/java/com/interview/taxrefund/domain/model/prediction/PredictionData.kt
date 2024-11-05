package com.interview.taxrefund.domain.model.prediction

import java.time.LocalDate


/**
 * Represents a tax refund processing time prediction
 * Combines estimated timeline and confidence based on historical patterns
 */
data class RefundPrediction(
    /**
     * Estimated number of days until refund is available
     * Calculated based on:
     * - Historical processing patterns (eg:40M records)
     * - Current IRS load
     * - Seasonal factors
     */
    val estimatedDays: Int,

    /**
     * Confidence level of the prediction (0.0 to 1.0)
     * Based on:
     * - Pattern strength
     * - Data quality
     * - Seasonal reliability
     * Higher values (>0.8) indicate more reliable predictions
     */
    val confidence: Float,

    /**
     * Expected completion date
     * Calculated as: Current Date + estimatedDays
     * Used for clear user communication
     */
    val estimatedDate: LocalDate,

    /**
     * List of factors affecting the prediction
     * Examples:
     * - Seasonal impact (tax season)
     * - System load
     * - Processing complexity
     * Used to explain prediction to users
     */
    val factors: List<DelayFactor>,

    /**
     * List of any issues affecting the refund
     * Optional: Only present if issues detected
     * Used to:
     * - Alert users of problems
     * - Provide resolution steps
     * - Show timeline impact
     */
    val issues: List<RefundIssue>? = null
)

/**
 * Represents factors that could cause processing delays
 */
data class DelayFactor(
    /**
     * Type of delay (SEASONAL, LOAD, SYSTEM, etc.)
     */
    val type: DelayType,

    /**
     * Impact level on processing time
     */
    val impact: ImpactLevel,

    /**
     * User-friendly explanation of the factor
     */
    val description: String
)

/**
 * Represents specific issues affecting the refund
 */
data class RefundIssue(
    /**
     * Type of issue (DOCUMENTATION, VERIFICATION, etc.)
     */
    val type: IssueType,

    /**
     * Issue severity (HIGH, MEDIUM, LOW)
     */
    val severity: Severity,

    /**
     * Impact on processing time
     */
    val timeImpact: TimeImpact,

    /**
     * Steps user can take to resolve the issue
     */
    val resolutionSteps: List<ResolutionStep>
)