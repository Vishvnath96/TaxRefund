package com.interview.taxrefund.core.metrics

import java.time.LocalDate

data class UserAction(
    val type: ActionType,
    val title: String,
    val description: String,
    val urgency: ActionUrgency,
    val deadline: LocalDate?
)

enum class ActionType {
    PROVIDE_DOCUMENT,
    CONTACT_IRS,
    VERIFY_IDENTITY,
    UPDATE_INFORMATION,
    WAIT_FOR_PROCESSING
}

data class UserGuidance(
    val step: Int,
    val title: String,
    val description: String,
    val action: UserAction?,
    val nextCheckDate: LocalDate?
)
