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
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PracticeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PracticeViewModel
    
    @Mock
    private lateinit var mockApplication: Application
    
    @Mock
    private lateinit var mockSharedPrefs: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        // Mock SharedPreferences using lenient() to avoid UnnecessaryStubbingException
        Mockito.lenient().`when`(mockApplication.getSharedPreferences("PracticeCompPrefs", Context.MODE_PRIVATE)).thenReturn(mockSharedPrefs)
        Mockito.lenient().`when`(mockSharedPrefs.getString(any(), any())).thenReturn(null)
        Mockito.lenient().`when`(mockSharedPrefs.edit()).thenReturn(mockEditor)
        Mockito.lenient().`when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        
        Dispatchers.setMain(testDispatcher)
        viewModel = PracticeViewModel(mockApplication)
        
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
        
        // First item should be the newest session (our change puts newest sessions first)
        assertEquals(7200000L, sessions[0].totalTimeMillis)
        assertEquals(3600000L, sessions[0].practiceTimeMillis)
        
        // Second item should be the older session
        assertEquals(3600000L, sessions[1].totalTimeMillis)
        assertEquals(1800000L, sessions[1].practiceTimeMillis)
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
    
    @Test
    fun `refreshSessions method exists and does not crash`() = runTest {
        // Simply verify the method exists and can be called without throwing exceptions
        viewModel.refreshSessions()
        // Method exists and did not crash
        testDispatcher.scheduler.advanceUntilIdle()
    }
} 