package com.interview.taxrefund.repository

import app.cash.turbine.test
import com.interview.taxrefund.core.common.Results
import com.interview.taxrefund.data.cache.CachedRefundStatus
import com.interview.taxrefund.data.cache.SecureRefundCache
import com.interview.taxrefund.data.remote.api.RefundApi
import com.interview.taxrefund.data.repository.RefundRepositoryImpl
import com.interview.taxrefund.domain.model.refund.RefundStatus
import com.interview.taxrefund.domain.repository.PredictionService
import com.interview.taxrefund.domain.syncmanager.SyncManager
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RefundRepositoryImplTest {

    @MockK
    private lateinit var api: RefundApi

    @MockK
    private lateinit var predictionService: PredictionService

    @MockK
    private lateinit var cache: SecureRefundCache

    @MockK
    private lateinit var syncManager: SyncManager

    private lateinit var repository: RefundRepositoryImpl
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        testDispatcher = StandardTestDispatcher()
        repository = RefundRepositoryImpl(
            api = api,
            predictionService = predictionService,
            cache = cache,
            syncManager = syncManager,
            dispatchers = testDispatcher
        )
    }

    @Test
    fun `getMostRecentRefundStatus should return cached data first if available`() = runTest {
        // Arrange
        val cachedStatus = mockk<RefundStatus>()
        val cachedData = mockk<CachedRefundStatus> {
            every { status } returns cachedStatus
            every { isStale(any()) } returns false
        }

        coEvery { cache.getStatus(any()) } returns cachedData

        // Act & Assert
        repository.getMostRecentRefundStatus().test {
            val result = awaitItem()
            assertTrue(result is Results.Success)
            assertEquals(cachedStatus, (result as Results.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun `getMostRecentRefundStatus should schedule refresh for stale cache`() = runTest {
        // Arrange
        val cachedStatus = mockk<RefundStatus> {
            every { id } returns "test-id"
            every { lastUpdated } returns Instant.now()
        }
        val cachedData = mockk<CachedRefundStatus> {
            every { status } returns cachedStatus
            every { isStale(any()) } returns true
        }

        coEvery { cache.getStatus(any()) } returns cachedData
        coEvery { syncManager.scheduleRefresh(any()) } just Runs

        // Act
        repository.getMostRecentRefundStatus().test {
            awaitItem()
            awaitComplete()
        }

        // Assert
        coVerify { syncManager.scheduleRefresh(cachedStatus.id) }
    }

    @Test
    fun `getMostRecentRefundStatus should fetch fresh data on cache miss`() = runTest {
        // Arrange
        val freshStatus = mockk<RefundStatus>()
        coEvery { cache.getStatus(any()) } returns null
        coEvery { api.getStatus(any(), any()) } returns mockk {
            every { toDomain() } returns freshStatus
        }
        coEvery { cache.saveStatus(any(), any()) } just Runs

        // Act & Assert
        repository.getMostRecentRefundStatus().test {
            val result = awaitItem()
            assertTrue(result is Results.Success)
            assertEquals(freshStatus, (result as Results.Success).data)
            awaitComplete()
        }
    }


    @Test
    fun `refreshRefundStatus should respect rate limiting`() = runTest {
        // Arrange
        coEvery { syncManager.canRefreshNow() } returns false

        // Act & Assert
        repository.refreshRefundStatus().test {
            val result = awaitItem()
            assertTrue(result is Results.Error)
            assertTrue((result as Results.Error).error is RateLimitException)
            awaitComplete()
        }
    }

    @Test
    fun `getPrediction should handle success case`() = runTest {
        // Arrange
        val refundId = "test-id"
        val prediction = mockk<RefundPrediction>()
        coEvery { predictionService.getPrediction(refundId) } returns prediction

        // Act & Assert
        repository.getPrediction(refundId).test {
            val result = awaitItem()
            assertTrue(result is Results.Success)
            assertEquals(prediction, (result as Results.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun `getPrediction should handle error case`() = runTest {
        // Arrange
        val refundId = "test-id"
        val error = Exception("Test error")
        coEvery { predictionService.getPrediction(refundId) } throws error

        // Act & Assert
        repository.getPrediction(refundId).test {
            val result = awaitItem()
            assertTrue(result is Results.Error)
            assertEquals(error, (result as Results.Error).exception)
            awaitComplete()
        }
    }

    @Test
    fun `clearData should clear cache and cancel syncs`() = runTest {
        // Arrange
        coEvery { cache.clear() } just Runs
        coEvery { syncManager.cancelAllRefresh() } just Runs

        // Act
        repository.clearData()

        // Assert
        coVerify {
            cache.clear()
            syncManager.cancelAllRefresh()
        }
    }
}