package com.interview.taxrefund.core.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PredictionConfig(
    // Only essential configurations for our scope
    val processing: ProcessingConfig = ProcessingConfig(),
    val confidence: ConfidenceConfig = ConfidenceConfig()
) {
    data class ProcessingConfig(
        val defaultProcessingDays: Int = 21,  // Default when no data
        val maxProcessingDays: Int = 45       // Upper limit
    )

    data class ConfidenceConfig(
        val minimum: Float = 0.4f,  // Minimum acceptable confidence
        val fallback: Float = 0.6f  // Fallback prediction confidence
    )
}