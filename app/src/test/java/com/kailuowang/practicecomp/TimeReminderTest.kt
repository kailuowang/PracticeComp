package com.kailuowang.practicecomp

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import java.lang.reflect.Method
import org.junit.Assert.*
import java.util.concurrent.TimeUnit

/**
 * Tests for the time reminder feature that announces progress at 25% intervals
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class TimeReminderTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var service: PracticeTrackingService
    private lateinit var testClock: TestClock
    private lateinit var mockTts: TextToSpeech
    private lateinit var ttsField: Field
    private lateinit var isTtsInitializedField: Field
    private lateinit var lastAnnouncedMilestoneField: Field
    private lateinit var checkProgressMilestonesMethod: Method
    private lateinit var checkGoalReachedMethod: Method
    
    // Test implementation of the Clock interface using the scheduler
    inner class TestClock(private val scheduler: TestCoroutineScheduler) : Clock {
        override fun getCurrentTimeMillis(): Long = scheduler.currentTime
    }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        testClock = TestClock(testDispatcher.scheduler)
        
        // Set up Text-to-Speech mock
        mockTts = Mockito.mock(TextToSpeech::class.java)
        
        // Set up the service
        service = PracticeTrackingService(clock = testClock)
        
        // Use reflection to inject our mocked TTS
        ttsField = PracticeTrackingService::class.java.getDeclaredField("textToSpeech")
        ttsField.isAccessible = true
        ttsField.set(service, mockTts)
        
        // Mark TTS as initialized
        isTtsInitializedField = PracticeTrackingService::class.java.getDeclaredField("isTtsInitialized")
        isTtsInitializedField.isAccessible = true
        isTtsInitializedField.setBoolean(service, true)
        
        // Access lastAnnouncedMilestone field for inspection
        lastAnnouncedMilestoneField = PracticeTrackingService::class.java.getDeclaredField("lastAnnouncedMilestone")
        lastAnnouncedMilestoneField.isAccessible = true
        
        // Access the checkProgressMilestones method
        checkProgressMilestonesMethod = PracticeTrackingService::class.java.getDeclaredMethod(
            "checkProgressMilestones", 
            Long::class.java, Long::class.java, Int::class.java
        )
        checkProgressMilestonesMethod.isAccessible = true
        
        // Reset the detection state before each test
        DetectionStateHolder.resetState()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `checkProgressMilestones WHEN 25 percent progress THEN announces 15 minutes left`() = runTest(testDispatcher) {
        // Set up 20-minute goal (1,200,000 ms)
        val goalMillis = TimeUnit.MINUTES.toMillis(20)
        
        // Simulate 25% progress
        val currentTime = goalMillis / 4 // 25% of goal (5 minutes elapsed)
        
        // Call the method directly using reflection
        checkProgressMilestonesMethod.invoke(service, currentTime, goalMillis, 20)
        
        // Verify TTS was called with correct message
        val textCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(mockTts).speak(textCaptor.capture(), eq(TextToSpeech.QUEUE_FLUSH), eq(null), anyString())
        
        val capturedText = textCaptor.value
        assertEquals("Good progress! 15 minutes left.", capturedText)
        
        // Verify milestone was updated
        assertEquals(1, lastAnnouncedMilestoneField.getInt(service))
    }
    
    @Test
    fun `checkProgressMilestones WHEN 50 percent progress THEN announces 10 minutes left`() = runTest(testDispatcher) {
        // Set last announced milestone to 1 (25% already announced)
        lastAnnouncedMilestoneField.setInt(service, 1)
        
        // Set up 20-minute goal
        val goalMillis = TimeUnit.MINUTES.toMillis(20)
        
        // Simulate 50% progress
        val currentTime = goalMillis / 2 // 50% of goal (10 minutes elapsed)
        
        // Call the method directly using reflection
        checkProgressMilestonesMethod.invoke(service, currentTime, goalMillis, 20)
        
        // Verify TTS was called with correct message
        val textCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(mockTts).speak(textCaptor.capture(), eq(TextToSpeech.QUEUE_FLUSH), eq(null), anyString())
        
        val capturedText = textCaptor.value
        assertEquals("Good progress! 10 minutes left.", capturedText)
        
        // Verify milestone was updated
        assertEquals(2, lastAnnouncedMilestoneField.getInt(service))
    }
    
    @Test
    fun `checkProgressMilestones WHEN 75 percent progress THEN announces 5 minutes left`() = runTest(testDispatcher) {
        // Set last announced milestone to 2 (50% already announced)
        lastAnnouncedMilestoneField.setInt(service, 2)
        
        // Set up 20-minute goal
        val goalMillis = TimeUnit.MINUTES.toMillis(20)
        
        // Simulate 75% progress
        val currentTime = goalMillis * 3 / 4 // 75% of goal (15 minutes elapsed)
        
        // Call the method directly using reflection
        checkProgressMilestonesMethod.invoke(service, currentTime, goalMillis, 20)
        
        // Verify TTS was called with correct message
        val textCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(mockTts).speak(textCaptor.capture(), eq(TextToSpeech.QUEUE_FLUSH), eq(null), anyString())
        
        val capturedText = textCaptor.value
        assertEquals("Good progress! 5 minutes left.", capturedText)
        
        // Verify milestone was updated
        assertEquals(3, lastAnnouncedMilestoneField.getInt(service))
    }
    
    @Test
    fun `checkProgressMilestones SHOULD NOT announce same milestone twice`() = runTest(testDispatcher) {
        // Set last announced milestone to 1 (25% already announced)
        lastAnnouncedMilestoneField.setInt(service, 1)
        
        // Set up 20-minute goal
        val goalMillis = TimeUnit.MINUTES.toMillis(20)
        
        // Simulate 30% progress (still in the 25% milestone)
        val currentTime = goalMillis * 30 / 100
        
        // Call the method directly using reflection
        checkProgressMilestonesMethod.invoke(service, currentTime, goalMillis, 20)
        
        // Verify TTS was NOT called (milestone already announced)
        Mockito.verify(mockTts, Mockito.never()).speak(anyString(), any(), any(), anyString())
        
        // Milestone should remain at 1
        assertEquals(1, lastAnnouncedMilestoneField.getInt(service))
    }
    
    @Test
    fun `resetTimerState SHOULD reset milestone tracking`() = runTest(testDispatcher) {
        // Set last announced milestone to 2 (50% already announced)
        lastAnnouncedMilestoneField.setInt(service, 2)
        
        // Get the resetTimerState method via reflection
        val resetTimerStateMethod = PracticeTrackingService::class.java.getDeclaredMethod("resetTimerState")
        resetTimerStateMethod.isAccessible = true
        
        // Call the method
        resetTimerStateMethod.invoke(service)
        
        // Verify milestone was reset to 0
        assertEquals(0, lastAnnouncedMilestoneField.getInt(service))
    }
} 