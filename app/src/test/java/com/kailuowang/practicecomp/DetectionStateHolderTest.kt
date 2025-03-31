package com.kailuowang.practicecomp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class DetectionStateHolderTest {

    @Before
    fun setup() {
        // Reset state before each test
        DetectionStateHolder.resetState()
    }

    @After
    fun tearDown() {
        // Reset state after each test for clean state
        DetectionStateHolder.resetState()
    }

    @Test
    fun `resetState creates fresh state with default values`() = runTest {
        // Given
        val initialTime = DetectionStateHolder.state.first().sessionStartTimeMillis
        DetectionStateHolder.updateState(
            newStatus = "Testing",
            newTimeMillis = 5000L,
            newTotalSessionTimeMillis = 10000L
        )
        
        // When
        DetectionStateHolder.resetState()
        val resetState = DetectionStateHolder.state.first()
        
        // Then
        assertEquals("Initializing...", resetState.statusMessage)
        assertEquals(0L, resetState.accumulatedTimeMillis)
        assertEquals(0L, resetState.totalSessionTimeMillis)
        // Session start time should be updated, but we can't guarantee it will be greater 
        // in fast-running tests, so just check that it's not zero
        assert(resetState.sessionStartTimeMillis > 0)
    }

    @Test
    fun `updateState with newTotalSessionTimeMillis updates session time`() = runTest {
        // Given
        val expectedSessionTime = 60000L
        
        // When
        DetectionStateHolder.updateState(
            newTotalSessionTimeMillis = expectedSessionTime
        )
        
        // Then
        val state = DetectionStateHolder.state.first()
        assertEquals(expectedSessionTime, state.totalSessionTimeMillis)
        // Other values should be unchanged
        assertEquals("Initializing...", state.statusMessage)
        assertEquals(0L, state.accumulatedTimeMillis)
    }

    @Test
    fun `updateState with newTimeMillis updates accumulated time`() = runTest {
        // Given
        val expectedAccumulatedTime = 30000L
        
        // When
        DetectionStateHolder.updateState(
            newTimeMillis = expectedAccumulatedTime
        )
        
        // Then
        val state = DetectionStateHolder.state.first()
        assertEquals(expectedAccumulatedTime, state.accumulatedTimeMillis)
        // Other values should be unchanged
        assertEquals("Initializing...", state.statusMessage)
        assertEquals(0L, state.totalSessionTimeMillis)
    }

    @Test
    fun `updateState with newStatus updates status message`() = runTest {
        // Given
        val expectedStatus = "Practicing"
        
        // When
        DetectionStateHolder.updateState(
            newStatus = expectedStatus
        )
        
        // Then
        val state = DetectionStateHolder.state.first()
        assertEquals(expectedStatus, state.statusMessage)
        // Other values should be unchanged
        assertEquals(0L, state.accumulatedTimeMillis)
        assertEquals(0L, state.totalSessionTimeMillis)
    }

    @Test
    fun `updateState can update all fields at once`() = runTest {
        // Given
        val expectedStatus = "Practicing"
        val expectedAccumulatedTime = 30000L
        val expectedSessionTime = 60000L
        
        // When
        DetectionStateHolder.updateState(
            newStatus = expectedStatus,
            newTimeMillis = expectedAccumulatedTime,
            newTotalSessionTimeMillis = expectedSessionTime
        )
        
        // Then
        val state = DetectionStateHolder.state.first()
        assertEquals(expectedStatus, state.statusMessage)
        assertEquals(expectedAccumulatedTime, state.accumulatedTimeMillis)
        assertEquals(expectedSessionTime, state.totalSessionTimeMillis)
    }
} 