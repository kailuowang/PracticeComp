package com.kailuowang.practicecomp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class PracticeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PracticeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PracticeViewModel()
        
        // Reset the state holder before each test
        DetectionStateHolder.resetState()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveSession creates and adds a new session to sessions flow`() = runTest {
        // Given
        val totalTimeMillis = 3600000L // 1 hour
        val practiceTimeMillis = 1800000L // 30 minutes
        
        // When
        viewModel.saveSession(totalTimeMillis, practiceTimeMillis)
        
        // Then
        val sessions = viewModel.sessions.first()
        assertEquals(1, sessions.size)
        assertEquals(totalTimeMillis, sessions[0].totalTimeMillis)
        assertEquals(practiceTimeMillis, sessions[0].practiceTimeMillis)
    }

    @Test
    fun `saveSession preserves previous sessions`() = runTest {
        // Given - Save a first session
        viewModel.saveSession(3600000L, 1800000L)
        
        // When - Save a second session
        viewModel.saveSession(7200000L, 3600000L)
        
        // Then
        val sessions = viewModel.sessions.first()
        assertEquals(2, sessions.size)
        assertEquals(7200000L, sessions[1].totalTimeMillis)
        assertEquals(3600000L, sessions[1].practiceTimeMillis)
    }

    @Test
    fun `formatted time in UI state shows correct values`() = runTest {
        // Add helper method to access private method directly
        val formatTimeMethod = PracticeViewModel::class.java.getDeclaredMethod("formatMillisToTimer", Long::class.java)
        formatTimeMethod.isAccessible = true
        
        // Test with 1 hour, 2 minutes, 3 seconds
        val testMillis = 3723000L
        val formatted = formatTimeMethod.invoke(viewModel, testMillis) as String
        
        assertEquals("01:02:03", formatted)
    }
} 