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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
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
        
        // Mock SharedPreferences
        `when`(mockApplication.getSharedPreferences("PracticeCompPrefs", Context.MODE_PRIVATE)).thenReturn(mockSharedPrefs)
        `when`(mockSharedPrefs.getString(any(), any())).thenReturn(null)
        `when`(mockSharedPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        
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
    fun `ending a session saves session data correctly`() = runTest {
        // Given - Session with some practice time
        val initialTime = testClock.getCurrentTimeMillis()
        service.setProcessingFlagForTest(true) // Simulate running service
        
        // Detect music for 10 minutes
        service.updateTimerState(detectedMusic = true, categoryLabel = "Music", score = 0.8f)
        testClock.advanceBy(600000) // 10 minutes
        service.updateUiTimer() // Update UI with current elapsed time
        
        // Stop music for 5 minutes
        service.updateTimerState(detectedMusic = false, categoryLabel = "Silence", score = 0.1f)
        testClock.advanceBy(300000) // 5 minutes
        service.updateUiTimer() // Update UI with accumulated time
        
        // Detect music again for 5 more minutes
        service.updateTimerState(detectedMusic = true, categoryLabel = "Music", score = 0.8f)
        testClock.advanceBy(300000) // 5 more minutes
        service.updateUiTimer() // Update UI with current elapsed time
        
        // Stop music again
        service.updateTimerState(detectedMusic = false, categoryLabel = "Silence", score = 0.1f)
        service.updateUiTimer() // Update accumulated time
        
        // Total session time: 20 minutes (1,200,000 ms)
        // Total practice time: 15 minutes (900,000 ms)
        
        // When - End the session and manually set session time (simulating what the service would do)
        DetectionStateHolder.updateState(
            newTotalSessionTimeMillis = 1200000L
        )
        
        viewModel.saveSession(
            totalTimeMillis = DetectionStateHolder.state.value.totalSessionTimeMillis,
            practiceTimeMillis = DetectionStateHolder.state.value.accumulatedTimeMillis
        )
        
        // Then - Check session was saved with correct values
        val sessions = viewModel.sessions.first()
        assertEquals(1, sessions.size)
        
        val savedSession = sessions[0]
        assertEquals(900000L, savedSession.practiceTimeMillis) // 15 minutes of practice
        assertEquals(1200000L, savedSession.totalTimeMillis) // 20 minutes total
        
        // Check percentage calculation
        assertEquals(75, savedSession.getPracticePercentage()) // 15/20 minutes = 75%
    }
    
    @Test
    fun `multiple sessions are tracked correctly`() = runTest {
        // First session: 30 minutes, 15 minutes practice
        service.setProcessingFlagForTest(true)
        service.setTimerStateForTest(isPlaying = false, startTime = 0L, accumulatedTime = 900000L)
        
        // Set the total session time (simulating what the service would track)
        DetectionStateHolder.updateState(
            newTimeMillis = 900000L,
            newTotalSessionTimeMillis = 1800000L
        )
        
        viewModel.saveSession(
            totalTimeMillis = 1800000L,
            practiceTimeMillis = 900000L
        )
        
        // Reset for second session
        DetectionStateHolder.resetState()
        service.setTimerStateForTest(isPlaying = false, startTime = 0L, accumulatedTime = 0L)
        
        // Second session: 60 minutes, 45 minutes practice
        testClock.advanceBy(3600000)
        service.setTimerStateForTest(isPlaying = false, startTime = 0L, accumulatedTime = 2700000L)
        
        // Set the total session time (simulating what the service would track)
        DetectionStateHolder.updateState(
            newTimeMillis = 2700000L,
            newTotalSessionTimeMillis = 3600000L
        )
        
        viewModel.saveSession(
            totalTimeMillis = 3600000L,
            practiceTimeMillis = 2700000L
        )
        
        // Then - Check both sessions were saved
        val sessions = viewModel.sessions.first()
        assertEquals(2, sessions.size)
        
        // Check first session values
        assertEquals(1800000L, sessions[0].totalTimeMillis)
        assertEquals(900000L, sessions[0].practiceTimeMillis)
        assertEquals(50, sessions[0].getPracticePercentage())
        
        // Check second session values
        assertEquals(3600000L, sessions[1].totalTimeMillis)
        assertEquals(2700000L, sessions[1].practiceTimeMillis)
        assertEquals(75, sessions[1].getPracticePercentage())
    }
} 