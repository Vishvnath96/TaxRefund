package com.interview.taxrefund.presentation.mvi.state

import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import com.interview.taxrefund.domain.model.refund.RefundStatus
import java.time.Duration
import java.time.Instant

data class RefundViewState(
    // Core Status Information
    val statusInfo: StatusInfo = StatusInfo(),

    // Prediction Information (when needed)
    val predictionInfo: PredictionInfo = PredictionInfo(),

    // UI State
    val uiState: UiState = UiState(),

    // User Guidance
    val guidance: GuidanceInfo = GuidanceInfo()
) {
    val isStale: Boolean
        get() = lastUpdated?.let {
            Duration.between(it, Instant.now()) > Duration.ofMinutes(15)
        } ?: true
}

data class StatusInfo(
    val status: RefundStatus? = null,
    val lastUpdated: Instant? = null,
    val isStale: Boolean = false
) {
    val shouldShowPrediction: Boolean
        get() = status?.needsPrediction == true
}

data class PredictionInfo(
    val prediction: RefundPrediction? = null,
    val state: PredictionState = PredictionState.None
)

data class UiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: RefundError? = null
)

data class GuidanceInfo(
    val message: String? = null,
    val processingDelay: ProcessingDelay? = null,
    val nextUpdateTime: Instant? = null
)
//-------------------------------------------------------------------
sealed class PredictionState {
    object None : PredictionState()
    object Loading : PredictionState()
    data class Available(
        val confidence: Float,
        val isHighConfidence: Boolean
    ) : PredictionState()
    data class Error(
        val type: PredictionErrorType
    ) : PredictionState()
}

data class ProcessingDelay(
    val reason: DelayReason,
    val estimatedDelay: Duration,
    val requiresAction: Boolean
)

enum class DelayReason {
    HIGH_VOLUME,
    SYSTEM_DELAY,
    ADDITIONAL_INFO_NEEDED,
    VERIFICATION_REQUIRED
}


data class LoadWarning(
    val message: String,
    val estimatedDelay: Duration
)


// Effects for UI actions
//-------------------------------------------------------------
sealed class RefundEffect {
    // Status Related
    data class ShowStatusUpdate(
        val message: String,
        val isError: Boolean = false
    ) : RefundEffect()

    // Processing Related
    data class ShowProcessingDelay(
        val delay: ProcessingDelay
    ) : RefundEffect()

    // Action Required
    data class RequireUserAction(
        val action: RequiredAction
    ) : RefundEffect()

    // System Related
    object ShowConnectivityWarning : RefundEffect()
    object ShowServiceUnavailable : RefundEffect()
}

// Required Actions
sealed class RequiredAction {
    object VerifyIdentity : RequiredAction()
    object ProvideAdditionalInfo : RequiredAction()
    data class ContactSupport(val reason: String) : RequiredAction()
    data class WaitAndRetry(val duration: Duration) : RequiredAction()
}

// Intent represents user actions
//-----------------------------------------------------------------------------
sealed class RefundIntent {
    object LoadStatus : RefundIntent()
    object RefreshStatus : RefundIntent()
    object RetryLastAction : RefundIntent()
    data class HandleUserAction(val action: RequiredAction) : RefundIntent()
}