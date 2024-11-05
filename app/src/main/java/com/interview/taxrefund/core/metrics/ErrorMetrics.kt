package com.interview.taxrefund.core.metrics

data class ErrorMetrics(
    val errorClass: String,
    val message: String?,
    val stackTrace: String,
    val metadata: Map<String, Any>?,
    val timestamp: Long)
