package com.interview.taxrefund.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * AuthInterceptor: Handles authentication for all network requests
 * Purpose:
 * 1. Centralize auth token management
 * 2. Add security headers consistently
 * 3. Handle token refresh if needed
 * 4. Track API requests for security
 */
class AuthInterceptor @Inject constructor(
    private val securityManager: SecurityManager,
    private val tokenRefreshManager: TokenRefreshManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Add authentication headers
        val modifiedRequest = originalRequest.newBuilder().apply {
            addHeader("Authorization", "Bearer ${securityManager.getToken()}")
            addHeader("Device-ID", securityManager.getDeviceId())
            addHeader("Request-ID", UUID.randomUUID().toString())
            addHeader("Timestamp", Instant.now().toString())
        }.build()

        // Proceed with the request
        var response = chain.proceed(modifiedRequest)

        // Handle 401 (Unauthorized) - Token might be expired
        if (response.code == 401 && !originalRequest.url.toString().contains("/refresh-token")) {
            response.close()

            // Try to refresh the token
            runBlocking {
                val newToken = tokenRefreshManager.refreshToken()
                // Retry with new token
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                response = chain.proceed(newRequest)
            }
        }

        return response
    }
}
