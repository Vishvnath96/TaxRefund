package com.interview.taxrefund.usecase

import app.cash.turbine.test
import com.interview.taxrefund.core.common.Results
import com.interview.taxrefund.core.config.PredictionConfig
import com.interview.taxrefund.domain.model.historical.ProcessingMetrics
import com.interview.taxrefund.domain.model.refund.RefundStatus
import com.interview.taxrefund.domain.repository.RefundRepository
import com.interview.taxrefund.domain.usecase.refund.GetRefundStatusUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class GetRefundStatusUseCaseTest {

    @MockK
    private lateinit var repository: RefundRepository

    @MockK
    private lateinit var config: PredictionConfig

    private lateinit var useCase: GetRefundStatusUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        useCase = GetRefundStatusUseCase(
            repository = repository,
            config = config,
            dispatchers = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `invoke with forceRefresh true should call refreshRefundStatus`() = runTest {
        // Arrange
        val status = mockk<RefundStatus>()
        val result = Results.Success(status)
        coEvery { repository.refreshRefundStatus() } returns flowOf(result)

        // Act
        val flow = useCase(forceRefresh = true)

        // Assert
        flow.test {
            assertEquals(result, awaitItem())
            awaitComplete()
        }
        coVerify { repository.refreshRefundStatus() }
        coVerify(exactly = 0) { repository.getMostRecentRefundStatus() }
    }

    @Test
    fun `invoke with forceRefresh false should call getMostRecentRefundStatus`() = runTest {
        // Arrange
        val status = mockk<RefundStatus>()
        val result = Results.Success(status)
        coEvery { repository.getMostRecentRefundStatus() } returns flowOf(result)

        // Act
        val flow = useCase(forceRefresh = false)

        // Assert
        flow.test {
            assertEquals(result, awaitItem())
            awaitComplete()
        }
        coVerify { repository.getMostRecentRefundStatus() }
        coVerify(exactly = 0) { repository.refreshRefundStatus() }
    }

    @Test
    fun `invoke should emit Error when exception occurs`() = runTest {
        // Arrange
        val exception = Exception("Test error")
        coEvery { repository.getMostRecentRefundStatus() } throws exception

        // Act
        val flow = useCase(forceRefresh = false)

        // Assert
        flow.test {
            assertEquals(Results.Error(exception), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeMetrics should return correct metrics for success result`() = runTest {
        // Arrange
        val status = mockk<RefundStatus>()
        val result = Results.Success(status)
        coEvery { repository.getMostRecentRefundStatus() } returns flowOf(result)

        val expectedMetrics = ProcessingMetrics(
            averageProcessingDays = 10f,
            currentLoadPercentage = 75f,
            estimatedWaitTime = Duration.ZERO,
            lastUpdated = Instant.now()
        )
        every { useCase.createMetricsFromStatus(status) } returns expectedMetrics

        // Act
        val flow = useCase.observeMetrics()

        // Assert
        flow.test {
            assertEquals(expectedMetrics, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeMetrics should return default metrics for error result`() = runTest {
        // Arrange
        val exception = Exception("Test error")
        val result = Results.Error(exception)
        coEvery { repository.getMostRecentRefundStatus() } returns flowOf(result)

        coEvery { config.processing.defaultProcessingDays } returns 21

        val expectedMetrics = ProcessingMetrics(
            averageProcessingDays = 21f,
            currentLoadPercentage = 0f,
            estimatedWaitTime = Duration.INFINITE,
            lastUpdated = Instant.now()
        )

        // Act
        val flow = useCase.observeMetrics()

        // Assert
        flow.test {
            assertEquals(expectedMetrics, awaitItem())
            awaitComplete()
        }
    }
}