package com.interview.taxrefund.core.common.extension

import com.interview.taxrefund.core.common.Results
import kotlinx.coroutines.flow.Flow

suspend fun <T> Flow<Results<T>>.collectResult(
    onSuccess: suspend (T) -> Unit,
    onError: suspend (Exception) -> Unit
) {
    collect { result ->
        when (result) {
            is Results.Success -> onSuccess(result.data)
            is Results.Error -> onError(result.exception)
        }
    }
}