package com.interview.taxrefund.domain.model.historical

import java.time.Instant
import kotlin.time.Duration


data class ProcessingMetrics(
    val averageProcessingDays: Float,
    val currentLoadPercentage: Float,
    val estimatedWaitTime: Duration,
    val lastUpdated: Instant
)