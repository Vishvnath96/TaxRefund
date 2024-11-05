package com.interview.taxrefund.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/**
 * ErrorInterceptor: Centralizes error handling for network requests
 * Purpose:
 * 1. Convert HTTP errors to domain-specific exceptions
 * 2. Handle retries for specific errors
 * 3. Log errors consistently
 * 4. Provide meaningful error messages
 */
class ErrorInterceptor @Inject constructor(
    private val errorLogger: ErrorLogger,
    private val networkUtils: NetworkUtils
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!networkUtils.isNetworkAvailable()) {
            throw NoConnectivityException()
        }

        val request = chain.request()
        try {
            val response = chain.proceed(request)

            when (response.code) {
                in 200..299 -> return response // Success cases
                401 -> {
                    errorLogger.logUnauthorized(request)
                    throw UnauthorizedException("Authentication required")
                }
                403 -> {
                    errorLogger.logForbidden(request)
                    throw ForbiddenException("Access denied")
                }
                404 -> {
                    errorLogger.logNotFound(request)
                    throw NotFoundException("Resource not found")
                }
                429 -> {
                    // Rate limiting - implement exponential backoff
                    val retryAfter = response.header("Retry-After")?.toLongOrNull()
                    throw RateLimitException(retryAfter)
                }
                in 500..599 -> {
                    errorLogger.logServerError(request, response.code)
                    throw ServerException(
                        "Server error occurred: ${response.code}",
                        response.code
                    )
                }
                else -> {
                    errorLogger.logUnexpectedError(request, response.code)
                    throw UnexpectedApiException(
                        "Unexpected error: ${response.code}"
                    )
                }
            }
        } catch (e: IOException) {
            throw NetworkException("Network error occurred", e)
        }
    }
}