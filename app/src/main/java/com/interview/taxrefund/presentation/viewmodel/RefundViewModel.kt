package com.interview.taxrefund.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interview.taxrefund.core.common.Results
import com.interview.taxrefund.core.config.PredictionConfig
import com.interview.taxrefund.domain.model.historical.ProcessingMetrics
import com.interview.taxrefund.domain.model.refund.RefundStatus
import com.interview.taxrefund.domain.model.refund.RefundStatusType
import com.interview.taxrefund.domain.syncmanager.SyncManager
import com.interview.taxrefund.domain.usecase.prediction.GetPredictionUseCase
import com.interview.taxrefund.domain.usecase.refund.GetRefundStatusUseCase
import com.interview.taxrefund.presentation.mvi.effect.RefundEffect
import com.interview.taxrefund.presentation.mvi.intent.RefundIntent
import com.interview.taxrefund.presentation.mvi.state.PredictionInfo
import com.interview.taxrefund.presentation.mvi.state.PredictionState
import com.interview.taxrefund.presentation.mvi.state.RefundViewState
import com.interview.taxrefund.presentation.mvi.state.StatusInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RefundViewModel @Inject constructor(
    private val getRefundStatusUseCase: GetRefundStatusUseCase,
    private val getPredictionUseCase: GetPredictionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RefundViewState())
    val state = _state.asStateFlow()

    private val _effect = Channel<RefundEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadInitialStatus()
    }

    // Called when ViewModel is created to:
    // 1. Show any cached status immediately
    // 2. Start loading fresh status if needed
    private fun loadInitialStatus() {
        viewModelScope.launch {
            try {
                // Start with loading state
                _state.update { it.copy(
                    uiState = it.uiState.copy(isLoading = true)
                )}

                // Get status using use case
                getRefundStatusUseCase(forceRefresh = false)
                    .collect { result ->
                        when (result) {
                            is Results.Success -> {
                                val status = result.data

                                // Update state with status
                                _state.update { current ->
                                    current.copy(
                                        statusInfo = StatusInfo(
                                            status = status,
                                            lastUpdated = Clock.System.now(),
                                            isStale = false
                                        ),
                                        uiState = current.uiState.copy(
                                            isLoading = false,
                                            error = null
                                        )
                                    )
                                }

                                // Get prediction if needed
                                if (status.needsPrediction) {
                                    loadPrediction(status.id)
                                }
                            }
                            is Results.Error -> {
                                _state.update { current ->
                                    current.copy(
                                        uiState = current.uiState.copy(
                                            isLoading = false,
                                            error = result.error
                                        )
                                    )
                                }
                                _effect.send(RefundEffect.ShowError(result.error))
                            }
                        }
                    }
            } catch (e: Exception) {
                _state.update { current ->
                    current.copy(
                        uiState = current.uiState.copy(
                            isLoading = false,
                            error = RefundError.UnknownError(e)
                        )
                    )
                }
                _effect.send(RefundEffect.ShowGenericError)
            }
        }
    }

    fun handleIntent(intent: RefundIntent) {
        viewModelScope.launch {
            when (intent) {
                is RefundIntent.LoadStatus -> {
                    loadStatus(forceRefresh = false)
                }
                is RefundIntent.RefreshStatus -> {
                    _state.update {
                        it.copy(uiState = it.uiState.copy(isRefreshing = true))
                    }
                    loadStatus(forceRefresh = true)
                }
                is RefundIntent.HandleUserAction -> {
                    handleUserAction(intent.action)
                }
                is RefundIntent.RetryLastAction -> {
                    retryLastAction()
                }
            }
        }
    }

    private suspend fun loadStatus(forceRefresh: Boolean) {
        getRefundStatusUseCase(forceRefresh)
            .onStart {
                _state.update {
                    it.copy(uiState = it.uiState.copy(isLoading = !forceRefresh))
                }
            }
            .collect { result ->
                when (result) {
                    is Results.Success -> {
                        handleStatusSuccess(result.data)
                    }
                    is Results.Error -> handleError(result.error)
                }
            }
    }

    private suspend fun handleStatusSuccess(status: RefundStatus) {
        _state.update { current ->
            current.copy(
                statusInfo = StatusInfo(
                    status = status,
                    lastUpdated = Clock.System.now(),
                    isStale = false
                ),
                uiState = current.uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null
                )
            )
        }

        // Check for prediction need
        if (status.needsPrediction) {
            loadPrediction(status.id)
        }
    }

    private suspend fun loadPrediction(refundId: String) {
        _state.update { it.copy(
            predictionInfo = it.predictionInfo.copy(
                state = PredictionState.Loading
            )
        )}

        getPredictionUseCase(refundId)
            .collect { result ->
                when (result) {
                    is Results.Success -> {
                        _state.update { current ->
                            current.copy(
                                predictionInfo = PredictionInfo(
                                    prediction = result.data,
                                    state = PredictionState.Available(
                                        confidence = result.data.confidence,
                                        isHighConfidence = result.data.confidence > 0.8f
                                    )
                                )
                            )
                        }
                    }
                    is Results.Error -> handlePredictionError(result.error)
                }
            }
    }
}