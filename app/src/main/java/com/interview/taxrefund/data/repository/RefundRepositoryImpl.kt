package com.interview.taxrefund.data.repository


import com.interview.taxrefund.core.common.Results
import com.interview.taxrefund.data.cache.SecureRefundCache
import com.interview.taxrefund.data.remote.api.RefundApi
import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import com.interview.taxrefund.domain.model.refund.RefundStatus
import com.interview.taxrefund.domain.model.refund.RefundStatusType
import com.interview.taxrefund.domain.repository.PredictionService
import com.interview.taxrefund.domain.repository.RefundRepository
import com.interview.taxrefund.domain.syncmanager.SyncManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefundRepositoryImpl @Inject constructor(
    private val api: RefundApi,
    private val predictionService: PredictionService,
    private val cache: SecureRefundCache,
    private val syncManager: SyncManager,
    private val dispatchers: CoroutineDispatcher
) : RefundRepository {

    /**
     * Get most recent tax refund status with prediction if needed
     * Handles:
     * - High traffic with caching
     * - IRS delays with fallback mechanisms
     * - Security with encryption
     */
    override suspend fun getMostRecentRefundStatus(): Flow<Results<RefundStatus>> = flow {
        // 1. Try cache first
        cache.getStatus(getCurrentRefundId())?.let { cached ->
            emit(Results.Success(cached.status))

            // If stale, refresh in background
            if (cached.isStale(cached.status.lastUpdated)) {
                syncManager.scheduleRefresh(cached.status.id)
            }
        }

        // 2. If no cache or needs refresh
        if (shouldFetchFresh()) {
            try {
                val status = fetchFreshStatus()
                emit(Results.Success(status))
            } catch (e: Exception) {
                when (e) {
                    is RateLimitException -> handleRateLimit(e)
                    is ServerBusyException -> handleServerBusy(e)
                    else -> handleError(e)
                }
            }
        }
    }.flowOn(dispatchers)

    /**
     * Force refresh status from IRS
     * Use when cached data is stale
     */
    override suspend fun refreshRefundStatus(): Flow<Results<RefundStatus>> = flow {
        try {
            // Check if we can refresh (rate limiting)
            if (!syncManager.canRefreshNow()) {
                throw RateLimitException("Too many requests")
            }

            val status = fetchFreshStatus()
            emit(Results.Success(status))
        } catch (e: Exception) {
            handleError(e)?.let { fallback ->
                emit(Results.Success(fallback))
            } ?: emit(Results.Error(e))
        }
    }.flowOn(dispatchers)

    private suspend fun fetchFreshStatus(): RefundStatus {
        // Get fresh status from API
        val response = api.getStatus("","")
        val status = response.toDomain()

        // Get prediction if needed
        val finalStatus = if (status.needsPrediction) {
            predictionService.getPrediction(status.id).fold(
                onSuccess = { prediction ->
                    status.copy(prediction = prediction)
                },
                onError = { status }
            )
        } else status

        // Save to cache
        cache.saveStatus(status.id, finalStatus)

        // Update sync schedule if needed
        if (status.status == RefundStatusType.PROCESSING) {
            syncManager.scheduleRefresh(status.id)
        } else {
            syncManager.cancelRefresh(status.id)
        }

        return finalStatus
    }

    /**
     * Get AI prediction for refund timing
     * Used when refund is not yet available
     */
    override suspend fun getPrediction(refundId: String): Flow<Results<RefundPrediction>> = flow {
        try {
            val prediction = predictionService.getPrediction(refundId)
            emit(Results.Success(prediction))
        } catch (e: Exception) {
            emit(Results.Error(e))
        }
    }.flowOn(dispatchers)

    /**
     * Clear sensitive data
     * Security requirement for data privacy
     */
    override suspend fun clearData() {
        cache.clear()
        syncManager.cancelAllRefresh()
    }

    private suspend fun handleError(error: Throwable): RefundStatus? {
        return when (error) {
            is RateLimitException -> {
                // Return cached data
                cache.getStatus(getCurrentRefundId())?.status
            }
            is NetworkException -> {
                // Return cached data marked as stale
                cache.getStatus(getCurrentRefundId())?.status?.copy(isStale = true)
            }
            else -> null
        }
    }

    private fun shouldFetchFresh(): Boolean {
        val cached = cache.getStatus(getCurrentRefundId())
        return when {
            cached == null -> true
            cached.isStale(cached.status.lastUpdated) -> true
            cached.status.status == RefundStatusType.PROCESSING -> true
            else -> false
        }
    }

    private suspend fun refreshWithServerGuidance() {
        // Handle server responses like:
        // - HTTP 429 (Too Many Requests)
        // - Retry-After headers
        // - Cache-Control directives
    }

    private fun getCurrentRefundId(): String {
        // In a real app, this would come from user session/preferences
        return "current_refund"
    }
}

/*
Key Points about the Design:

1. PredictionService Responsibilities:
- AI model interaction
- Metrics handling
- Fallback predictions
- Prediction caching
- Accuracy tracking

2. Repository Responsibilities:
- Status management
- Cache coordination
- Prediction coordination
- Data cleanup

3. Separation of Concerns:
PredictionService:
- Focused on prediction logic
- Handles AI interaction
- Manages prediction accuracy

Repository:
- Coordinates overall flow
- Manages status cache
- Delegates prediction work
```

4. Flow of Operations:
Get Status:
1. Check cache
2. Return cached if fresh
3. Refresh if stale
4. Get prediction if needed

Refresh Status:
1. Get fresh status
2. Get prediction if needed
3. Update cache
4. Return result

Get Prediction:
1. Delegate to prediction service
2. Handle results

 */