package com.interview.taxrefund.core.common

sealed class Results<out T> {
    data class Success<T>(val data: T) : Results<T>()
    data class Error(val exception: Exception) : Results<Nothing>()

    fun isSuccess() = this is Success
    fun isError() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    companion object {
        fun <T> success(data: T): Results<T> = Success(data)
        fun error(exception: Exception): Results<Nothing> = Error(exception)
    }
}

