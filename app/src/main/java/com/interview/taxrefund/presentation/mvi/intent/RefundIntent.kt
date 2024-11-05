package com.interview.taxrefund.presentation.mvi.intent

sealed class RefundIntent {
    object LoadInitialStatus : RefundIntent()
    object RefreshStatus : RefundIntent()
    data class CheckSpecificRefund(val refundId: String) : RefundIntent()
    object RetryLastFailedAction : RefundIntent()
    object ClearError : RefundIntent()
}