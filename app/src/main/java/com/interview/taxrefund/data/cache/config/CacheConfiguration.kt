package com.interview.taxrefund.data.cache.config

import java.time.Duration

data class CacheConfiguration(
    val maxSize: Long,
    val expireAfterWrite: Duration,
    val expireAfterAccess: Duration
)