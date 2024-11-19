package com.interview.taxrefund.usecase

import app.cash.turbine.test
import com.interview.taxrefund.core.common.Results
import com.interview.taxrefund.core.config.PredictionConfig
import com.interview.taxrefund.domain.model.prediction.RefundPrediction
import com.interview.taxrefund.domain.repository.RefundRepository
import com.interview.taxrefund.domain.usecase.prediction.GetPredictionUseCase
import com.interview.taxrefund.domain.usecase.prediction.InvalidPredictionException
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GetPredictionUseCaseTest {

    @MockK
    private lateinit var repository: RefundRepository

    @MockK
    private lateinit var config: PredictionConfig

    @MockK
    private lateinit var processingConfig: PredictionConfig.ProcessingConfig

    @MockK
    private lateinit var confidenceConfig: PredictionConfig.ConfidenceConfig

    private lateinit var useCase: GetPredictionUseCase
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        testDispatcher = StandardTestDispatcher()

        every { config.processing } returns processingConfig
        every { config.confidence } returns confidenceConfig

        useCase = GetPredictionUseCase(
            repository = repository,
            config = config,
            dispatchers = testDispatcher
        )
    }

    @Test
    fun `invoke should return success when prediction is valid`() = runTest {
        // Arrange
        val refundId = "test-id"
        val prediction = RefundPrediction(
            estimatedDays = 15,
            confidence = 0.9f,
            estimatedDate = LocalDate.now(),
            factors = emptyList()
        )

        every { processingConfig.maxProcessingDays } returns 20
        every { confidenceConfig.minimum } returns 0.4f

        coEvery {
            repository.getPrediction(refundId)
        } returns flowOf(Results.Success(prediction))

        // Act & Assert
        useCase(refundId).test {
            val result = awaitItem()
            assertTrue(result is Results.Success)
            assertEquals(prediction, (result as Results.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun `invoke should return InvalidPredictionException when days outside range`() = runTest {
        // Arrange
        val refundId = "test-id"
        val prediction = RefundPrediction(
            estimatedDays = 25, // Outside range
            confidence = 0.9f,
            estimatedDate = LocalDate.now(),
            factors = emptyList()
        )

        every { processingConfig.maxProcessingDays } returns 20

        coEvery {
            repository.getPrediction(refundId)
        } returns flowOf(Results.Success(prediction))

        // Act & Assert
        useCase(refundId).test {
            val result = awaitItem()
            assertTrue(result is Results.Error)
            assertTrue((result as Results.Error).exception is InvalidPredictionException)
            awaitComplete()
        }
    }

    @Test
    fun `invoke should propagate repository errors`() = runTest {
        // Arrange
        val refundId = "test-id"
        val error = Exception("Repository error")

        coEvery {
            repository.getPrediction(refundId)
        } returns flowOf(Results.Error(error))

        // Act & Assert
        useCase(refundId).test {
            val result = awaitItem()
            assertTrue(result is Results.Error)
            assertEquals(error, (result as Results.Error).exception)
            awaitComplete()
        }
    }
}