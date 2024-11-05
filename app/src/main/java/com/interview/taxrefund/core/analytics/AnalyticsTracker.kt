package com.interview.taxrefund.core.analytics

import com.interview.taxrefund.core.metrics.ApiMetrics

interface AnalyticsTracker {
    fun trackApiCall(metrics: ApiMetrics)
    fun trackError(error: Throwable, metadata: Map<String, Any>? = null)
    fun trackUserAction(action: String, parameters: Map<String, Any>? = null)
    fun trackRefreshSuccess(refundId: String) {

    }

    fun trackRefreshError(refundId: String, error: Any) {

    }
}