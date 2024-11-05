package com.interview.taxrefund.data.cache.extensions

import com.github.benmanes.caffeine.cache.Cache


suspend fun <K, V> Cache<K, V>.getOrPut(
    key: K,
    defaultValue: suspend () -> V
): V {
    return getIfPresent(key) ?: defaultValue().also { value ->
        put(key, value)
    }
}