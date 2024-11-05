package com.interview.taxrefund.data.repository

import kotlinx.coroutines.delay

data class RetryPolicy(
    val maxAttempts: Int,
    val initialDelay: kotlin.time.Duration,
    val maxDelay: kotlin.time.Duration,
    val factor: Double = 2.0
)

suspend fun <T> withRetry(
    policy: RetryPolicy,
    block: suspend () -> T
): T {
    var currentDelay = policy.initialDelay
    repeat(policy.maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (!e.isRetryable()) throw e

            delay(currentDelay.inWholeMilliseconds)
            currentDelay = (currentDelay.multipliedBy(policy.factor.toLong()))
                .coerceAtMost(policy.maxDelay)
        }
    }
    return block() // Last attempt
}