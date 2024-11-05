package com.interview.taxrefund.presentation.mvi.effect

sealed class RefundEffect {
    data class ShowToast(val message: String) : RefundEffect()
    data class ShowError(val error: RefundError) : RefundEffect()
    data class ScheduleSync(val refundId: String) : RefundEffect()
    object ShowRefreshIndicator : RefundEffect()
    data class NavigateToSupport(val issueId: String? = null) : RefundEffect()
}
