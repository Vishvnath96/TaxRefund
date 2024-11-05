package com.interview.taxrefund.core.network

import android.util.Log
import com.interview.taxrefund.BuildConfig
import com.interview.taxrefund.core.metrics.ApiMetrics
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * LoggingInterceptor: Custom implementation for detailed logging
 * Purpose:
 * 1. Detailed API debugging
 * 2. Performance monitoring
 * 3. Security audit logging
 * 4. Rate limit monitoring
 */
class LoggingInterceptor @Inject constructor(
    private val analyticsTracker: AnalyticsTracker
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        // Log request
        logRequest(request)

        // Proceed with the request
        val response = chain.proceed(request)
        val endTime = System.nanoTime()

        // Calculate request duration
        val duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)

        // Log response
        logResponse(response, duration)

        // Track API metrics
        trackApiMetrics(request.url.toString(), duration, response.code)

        return response
    }

    private fun logRequest(request: Request) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Request: ${request.method} ${request.url}")
            Log.d(TAG, "Headers: ${request.headers}")
            request.body?.let {
                Log.d(TAG, "Body: ${it.toString()}")
            }
        }
    }

    private fun logResponse(response: Response, duration: Long) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Response Code: ${response.code}")
            Log.d(TAG, "Duration: ${duration}ms")
            Log.d(TAG, "Headers: ${response.headers}")
        }
    }

    private fun trackApiMetrics(url: String, duration: Long, statusCode: Int) {
        analyticsTracker.trackApiCall(
            ApiMetrics(
                url = url,
                duration = duration,
                statusCode = statusCode,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    companion object {
        private const val TAG = "API_LOG"
    }
}