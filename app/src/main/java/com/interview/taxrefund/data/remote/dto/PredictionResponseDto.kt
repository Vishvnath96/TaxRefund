package com.interview.taxrefund.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class PredictionResponseDto(
    @Json(name = "estimated_days") val estimatedDays: Int,
    @Json(name = "confidence") val confidence: Float,
    @Json(name = "estimated_date") val estimatedDate: String,
    @Json(name = "factors") val factors: List<DelayFactorDto>
)

