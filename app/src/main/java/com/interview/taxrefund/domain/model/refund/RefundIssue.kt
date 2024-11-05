package com.interview.taxrefund.domain.model.refund

import com.interview.taxrefund.core.metrics.UserAction
import java.time.Duration

data class RefundIssue(
    val code: String,
    val title: String,
    val description: String,
    val severity: IssueSeverity,
    val userActions: List<UserAction>,
    val estimatedResolutionTime: Duration?
)
enum class IssueSeverity {
    INFO, WARNING, ERROR
}