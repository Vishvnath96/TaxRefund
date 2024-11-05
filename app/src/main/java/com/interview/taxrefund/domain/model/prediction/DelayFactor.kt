package com.interview.taxrefund.domain.model.prediction



enum class DelayType {
    VERIFICATION_NEEDED,
    HIGH_VOLUME,
    SYSTEM_DELAY,
    ADDITIONAL_REVIEW
}

enum class ImpactLevel {
    LOW, MEDIUM, HIGH
}