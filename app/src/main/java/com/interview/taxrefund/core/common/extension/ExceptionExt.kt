package com.interview.taxrefund.core.common.extension

import com.interview.taxrefund.data.repository.RefundRepositoryImpl
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

fun Exception.isRetryable(): Boolean {
    return when (this) {
        is IOException,
        is SocketTimeoutException,
        is ConnectException,
        is RefundRepositoryImpl.RefundError.IrsTemporaryError -> true
        is HttpException -> code() in 500..599
        else -> false
    }
}