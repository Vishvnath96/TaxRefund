package com.interview.taxrefund.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class RefundStatusDto(
    @Json(name = "id") val id: String,
    @Json(name = "status") val status: String,
    @Json(name = "amount") val amount: String?,
    @Json(name = "filing_date") val filingDate: String,
    @Json(name = "last_updated") val lastUpdated: String,
    @Json(name = "prediction") val prediction: PredictionResponseDto?,
    @Json(name = "issues") val issues: List<RefundIssueDto>? = null
)

@JsonClass(generateAdapter = true)
data class RefundIssueDto(
    @Json(name = "code") val code: String,
    @Json(name = "description") val description: String,
    @Json(name = "severity") val severity: String,
    @Json(name = "resolution") val resolution: String?
)


data class IssueDto(
    @Json(name = "code") val code: String,
    @Json(name = "description") val description: String,
    @Json(name = "severity") val severity: String,
    @Json(name = "resolution") val resolution: String?
)

