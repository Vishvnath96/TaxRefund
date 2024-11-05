package com.interview.taxrefund.presentation.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.interview.taxrefund.core.metrics.UserAction
import com.interview.taxrefund.core.metrics.UserGuidance
import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import com.interview.taxrefund.domain.model.refund.RefundStatus
import com.interview.taxrefund.presentation.mvi.effect.RefundEffect
import com.interview.taxrefund.presentation.mvi.intent.RefundIntent
import com.interview.taxrefund.presentation.viewmodel.RefundViewModel
import java.time.format.DateTimeFormatter

@Composable
fun RefundStatusScreen(
    viewModel: RefundViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        // Initial load when screen opens
        viewModel.handleIntent(RefundIntent.LoadInitialStatus)
    }

    Scaffold(
        topBar = { RefundStatusTopBar() }
    ) { padding ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(state.refreshState.isRefreshing),
            onRefresh = viewModel::onRefresh
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Status Section
                when (val statusState = state.status) {
                    StatusState.Initial,
                    StatusState.Loading -> LoadingView()

                    is StatusState.Success -> {
                        RefundStatusView(
                            status = statusState.status,
                            lastUpdated = statusState.lastUpdated,
                            isStale = statusState.isStale
                        )
                    }

                    is StatusState.Error -> {
                        ErrorView(error = statusState.error)
                    }
                }

                // Prediction Section
                when (val predictionState = state.prediction) {
                    PredictionState.Loading -> PredictionLoadingView()

                    is PredictionState.Available -> {
                        PredictionView(
                            prediction = predictionState.prediction,
                            isHighConfidence = predictionState.isHighConfidence
                        )
                    }

                    is PredictionState.Error -> {
                        PredictionErrorView(error = predictionState.error)
                    }

                    PredictionState.None -> { /* Nothing to show */ }
                }
            }
        }
    }
}

@Composable
private fun RefundStatusView(
    status: RefundStatus,
    lastUpdated: Instant,
    isStale: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status Header
            StatusHeader(status = status)

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Status Timeline
            StatusTimeline(status = status)

            // Last Updated Info
            LastUpdatedInfo(
                timestamp = lastUpdated,
                isStale = isStale
            )

            // Issues (if any)
            if (status.issues.isNotEmpty()) {
                IssuesList(issues = status.issues)
            }
        }
    }
}

@Composable
private fun PredictionView(
    prediction: RefundPrediction,
    isHighConfidence: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Estimated Processing Time",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${prediction.estimatedDays} days",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Expected by ${prediction.estimatedDate.format(
                            DateTimeFormatter.ofPattern("MMM dd, yyyy")
                        )}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                ConfidenceIndicator(
                    confidence = prediction.confidence,
                    isHigh = isHighConfidence
                )
            }

            if (prediction.factors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                DelayFactorsList(factors = prediction.factors)
            }
        }
    }
}

//@Composable
//fun RefundStatusScreen(
//    viewModel: RefundViewModel = hiltViewModel(),
//    onNavigateToSupport: (String?) -> Unit
//) {
//    val state by viewModel.state.collectAsStateWithLifecycle()
//
//    Scaffold(
//        topBar = {
//            RefundStatusTopBar(
//                onRefresh = { viewModel.submitIntent(RefundIntent.RefreshStatus) }
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .padding(padding)
//                .fillMaxSize()
//        ) {
//            // Status Card - Always visible
//            RefundStatusCard(
//                status = state.refundStatus,
//                isRefreshing = state.isRefreshing,
//                onRefresh = { viewModel.submitIntent(RefundIntent.RefreshStatus) }
//            )
//
//            // Main Content
//            when {
//                state.isLoading && state.refundStatus == null -> {
//                    LoadingContent()
//                }
//                state.error != null -> {
//                    ErrorContentWithGuidance(
//                        error = state.error,
//                        cachedStatus = state.refundStatus,
//                        onRetry = { viewModel.submitIntent(RefundIntent.RetryLastFailedAction) },
//                        onContactSupport = onNavigateToSupport
//                    )
//                }
//                state.refundStatus != null -> {
//                    RefundStatusContent(
//                        status = state.refundStatus,
//                        prediction = state.prediction,
//                        guidance = state.guidance,
//                        onAction = { action ->
//                            viewModel.submitIntent(RefundIntent.HandleUserAction(action))
//                        }
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun RefundStatusContent(
//    status: RefundStatus,
//    prediction: RefundPrediction?,
//    guidance: List<UserGuidance>,
//    onAction: (UserAction) -> Unit
//) {
//    LazyColumn {
//        // Status Timeline
//        item {
//            ProcessingTimeline(
//                currentStage = status.processingStage,
//                prediction = prediction
//            )
//        }
//
//        // Prediction Section (if applicable)
//        if (prediction != null) {
//            item {
//                PredictionCard(
//                    prediction = prediction,
//                    modifier = Modifier.padding(16.dp)
//                )
//            }
//        }
//
//        // User Guidance Section
//        items(guidance) { step ->
//            GuidanceCard(
//                guidance = step,
//                onAction = onAction,
//                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun GuidanceCard(
//    guidance: UserGuidance,
//    onAction: (UserAction) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Card(
//        modifier = modifier,
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            Text(
//                text = "Step ${guidance.step}: ${guidance.title}",
//                style = MaterialTheme.typography.titleMedium
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Text(
//                text = guidance.description,
//                style = MaterialTheme.typography.bodyMedium
//            )
//
//            guidance.action?.let { action ->
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Button(
//                    onClick = { onAction(action) },
//                    modifier = Modifier.align(Alignment.End)
//                ) {
//                    Text(action.title)
//                }
//            }
//
//            guidance.nextCheckDate?.let { date ->
//                Spacer(modifier = Modifier.height(8.dp))
//                Text(
//                    text = "Next check: ${date.format(DateTimeFormatter.ofPattern("MMM dd"))}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        }
//    }
//}