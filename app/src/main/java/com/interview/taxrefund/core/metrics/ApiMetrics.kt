package com.interview.taxrefund.core.metrics

import java.util.UUID

data class ApiMetrics(
    val url: String,
    val duration: Long,
    val statusCode: Int,
    val timestamp: Long,
    val requestId: String = UUID.randomUUID().toString(),
    val method: String? = null,
    val errorMessage: String? = null
)
