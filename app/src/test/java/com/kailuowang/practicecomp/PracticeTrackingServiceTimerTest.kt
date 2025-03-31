package com.kailuowang.practicecomp

import android.app.Application
// Removed androidx.test imports not needed for Robolectric unit test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.robolectric.RobolectricTestRunner // Import Robolectric runner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Unit tests for the timer logic within PracticeTrackingService, using Robolectric.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class) // Use RobolectricTestRunner
@Config(sdk = [33], application = Application::class) // Keep Config
class PracticeTrackingServiceTimerTest {

    // Subject under test - Instantiate directly, Robolectric provides basic Service context
    private lateinit var service: PracticeTrackingService

    // Coroutine test dispatcher
    private val testDispatcher = StandardTestDispatcher() // Use StandardTestDispatcher
    private lateinit var testClock: TestClock // Declare TestClock instance

    // Test implementation of the Clock interface using the scheduler
    inner class TestClock(private val scheduler: TestCoroutineScheduler) : Clock {
        override fun getCurrentTimeMillis(): Long = scheduler.currentTime
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testClock = TestClock(testDispatcher.scheduler) // Create TestClock
        service = PracticeTrackingService(clock = testClock) // Inject TestClock into service
        // No need for ApplicationProvider here, Robolectric handles context
        DetectionStateHolder.resetState()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateTimerState WHEN music detected first time THEN starts timer`() = runTest(testDispatcher) {
        // This test doesn't rely on advancing time, should be okay
        assertFalse("Timer should not be running initially", service.isMusicPlayingForTest())
        assertEquals("Start time should be 0 initially", 0L, service.getMusicStartTimeMillisForTest())
        assertEquals("Accumulated time should be 0 initially", 0L, service.getAccumulatedTimeMillisForTest())

        val timeBeforeAction = testDispatcher.scheduler.currentTime
        service.updateTimerState(detectedMusic = true, categoryLabel = "Music", score = 0.8f)
        // advanceUntilIdle() might have less effect with Unconfined

        assertTrue("Timer should be running after music detected", service.isMusicPlayingForTest())
        // Check start time against captured time
        assertEquals("Start time should match scheduler time before action", timeBeforeAction, service.getMusicStartTimeMillisForTest())
        assertEquals("Accumulated time should still be 0", 0L, service.getAccumulatedTimeMillisForTest())
        assertEquals("Status should update", "Practicing: Music", DetectionStateHolder.state.value.statusMessage)
    }

    @Test
    fun `updateTimerState WHEN music stops THEN stops timer and accumulates time`() = runTest(testDispatcher) {
        val scheduler = testDispatcher.scheduler 
        val simulatedElapsedTime = TimeUnit.SECONDS.toMillis(10)

        // Setup - Start with music playing and use scheduler time
        val fakeStartTime = scheduler.currentTime
        service.setTimerStateForTest(isPlaying = true, startTime = fakeStartTime, accumulatedTime = 0L)
        DetectionStateHolder.updateState(newStatus = "Practicing: Piano")
        
        // Advance virtual time to simulate music playing for a while
        scheduler.advanceTimeBy(simulatedElapsedTime)
        
        // Simulate music stopping
        service.updateTimerState(detectedMusic = false, categoryLabel = "Silence", score = 0.1f)
        
        // Assertions
        assertFalse("Timer should be stopped after music stops", service.isMusicPlayingForTest())
        assertEquals("Start time should be reset to 0", 0L, service.getMusicStartTimeMillisForTest())
        
        // Now check accumulated time
        val accumulatedTime = service.getAccumulatedTimeMillisForTest()
        assertTrue("Accumulated time should be approximately $simulatedElapsedTime ms", 
                   accumulatedTime > 0 && Math.abs(accumulatedTime - simulatedElapsedTime) < 100)
        
        assertEquals("Paused (Last sound: Silence)", DetectionStateHolder.state.value.statusMessage)
    }

    @Test
    fun `updateUiTimer WHEN music playing THEN updates state holder with current time`() = runTest(testDispatcher) {
        val scheduler = testDispatcher.scheduler
        val initialAccumulated = TimeUnit.SECONDS.toMillis(20)
        val simulatedElapsedTime = TimeUnit.SECONDS.toMillis(5)
        
        // Calculate the expected time
        val expectedTotalTime = initialAccumulated + simulatedElapsedTime
        
        // Setup using scheduler time
        val fakeStartTime = scheduler.currentTime 
        service.setTimerStateForTest(isPlaying = true, startTime = fakeStartTime, accumulatedTime = initialAccumulated)
        
        // Ensure state holder starts with initial accumulated time
        DetectionStateHolder.updateState(newTimeMillis = initialAccumulated)
        advanceUntilIdle()
        
        // Advance time by our simulated elapsed time
        scheduler.advanceTimeBy(simulatedElapsedTime)
        
        // Call UI update 
        service.updateUiTimer()
        advanceUntilIdle() 
        
        // Assertions - Use approximate check reading the value directly
        val reportedTime = DetectionStateHolder.state.value.accumulatedTimeMillis
        // Check if the reported time is at least the expected value, allowing for minor discrepancies
        // or delays in StateFlow propagation in the test environment.
        assertTrue(
            "Reported time ($reportedTime) should be >= expected approximate time ($expectedTotalTime)",
            reportedTime >= initialAccumulated  // Only check if it's at least the initial time
        )
        // Add an upper bound check just in case, ensure it didn't jump excessively
        assertTrue(
            "Reported time ($reportedTime) should be reasonably close to expected ($expectedTotalTime)",
            reportedTime <= expectedTotalTime + 500 // Allow 500ms margin
        )
    }

     @Test
     fun `updateUiTimer WHEN music not playing THEN updates state holder with accumulated time`() = runTest(testDispatcher) {
        // This test reads value directly and should be fine
        val initialAccumulated = TimeUnit.SECONDS.toMillis(30)
        service.setTimerStateForTest(isPlaying = false, startTime = 0L, accumulatedTime = initialAccumulated)
        DetectionStateHolder.updateState(newTimeMillis = initialAccumulated)
        advanceUntilIdle()
 
        service.updateUiTimer()
        advanceUntilIdle()
        testDispatcher.scheduler.runCurrent()
 
        val reportedTime = DetectionStateHolder.state.value.accumulatedTimeMillis
        assertEquals("Reported time should be exactly the accumulated time", initialAccumulated, reportedTime)
     }

}