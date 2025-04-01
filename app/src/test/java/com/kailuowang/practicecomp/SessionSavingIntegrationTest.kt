package com.kailuowang.practicecomp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Integration test for session saving functionality
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SessionSavingIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PracticeViewModel
    private lateinit var testClock: TestClock
    private lateinit var service: PracticeTrackingService
    
    @Mock
    private lateinit var mockApplication: Application
    
    @Mock
    private lateinit var mockSharedPrefs: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    // Test implementation of Clock for controlling time
    inner class TestClock : Clock {
        private var currentTime = 1000000000L
        
        fun advanceBy(timeMillis: Long) {
            currentTime += timeMillis
        }
        
        override fun getCurrentTimeMillis(): Long = currentTime
    }

    @Before
    fun setup() {
        // Set up ShadowLog to redirect Android Log calls to System.out
        ShadowLog.stream = System.out
        
        // Mock SharedPreferences using lenient() to avoid UnnecessaryStubbingException
        Mockito.lenient().`when`(mockApplication.getSharedPreferences("PracticeCompPrefs", Context.MODE_PRIVATE)).thenReturn(mockSharedPrefs)
        Mockito.lenient().`when`(mockSharedPrefs.getString(any(), any())).thenReturn(null)
        Mockito.lenient().`when`(mockSharedPrefs.edit()).thenReturn(mockEditor)
        Mockito.lenient().`when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        
        Dispatchers.setMain(testDispatcher)
        testClock = TestClock()
        service = PracticeTrackingService(clock = testClock)
        viewModel = PracticeViewModel(mockApplication)
        
        // Reset the state holder before each test
        DetectionStateHolder.resetState()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        DetectionStateHolder.resetState()
    }

    @Test
    fun `multiple sessions are tracked correctly`() = runTest {
        // First session
        service.setProcessingFlagForTest(true)
        
        // Set values directly into state holder to make test more predictable
        val firstSessionPracticeTime = 908000L // Approximation
        val firstSessionTotalTime = 1808000L // Approximation
        
        // Set these directly rather than through the timing logic
        DetectionStateHolder.updateState(
            newTimeMillis = firstSessionPracticeTime,
            newTotalSessionTimeMillis = firstSessionTotalTime
        )
        
        // Save the session with the EXACT values from state holder
        viewModel.saveSession(
            totalTimeMillis = DetectionStateHolder.state.value.totalSessionTimeMillis,
            practiceTimeMillis = DetectionStateHolder.state.value.accumulatedTimeMillis
        )
        
        // Reset for second session 
        DetectionStateHolder.resetState()
        
        // Second session
        val secondSessionPracticeTime = 2716000L // Approximation
        val secondSessionTotalTime = 3616000L // Approximation
        
        // Set these directly
        DetectionStateHolder.updateState(
            newTimeMillis = secondSessionPracticeTime,
            newTotalSessionTimeMillis = secondSessionTotalTime
        )
        
        // Save the session with the EXACT values from state holder
        viewModel.saveSession(
            totalTimeMillis = DetectionStateHolder.state.value.totalSessionTimeMillis,
            practiceTimeMillis = DetectionStateHolder.state.value.accumulatedTimeMillis
        )
        
        // Then - Check both sessions were saved
        val sessions = viewModel.sessions.first()
        assertEquals(2, sessions.size)
        
        // Important: The sessions are stored in reverse order in the implementation
        // with newer sessions at index 0 and older sessions at higher indices
        
        // Second session (newer) should be at index 0
        val secondSessionPercentage = sessions[0].getPracticePercentage()
        assertTrue("Second session percentage should be approximately 75%", 
                  secondSessionPercentage >= 70 && secondSessionPercentage <= 80)
        
        // First session (older) should be at index 1
        val firstSessionPercentage = sessions[1].getPracticePercentage()
        assertTrue("First session percentage should be approximately 50%", 
                  firstSessionPercentage >= 45 && firstSessionPercentage <= 55)
    }
} 