package com.kailuowang.practicecomp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.junit.Assert.*

/**
 * Tests for the goal tracking functionality
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GoalTrackingTest {

    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var mockApplication: Application
    
    @Mock
    private lateinit var mockSharedPrefs: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        // Mock SharedPreferences
        Mockito.lenient().`when`(mockApplication.getSharedPreferences("PracticeCompPrefs", Context.MODE_PRIVATE)).thenReturn(mockSharedPrefs)
        Mockito.lenient().`when`(mockSharedPrefs.getString(any(), any())).thenReturn(null)
        Mockito.lenient().`when`(mockSharedPrefs.getInt("practice_goal_minutes", 0)).thenReturn(0)
        Mockito.lenient().`when`(mockSharedPrefs.edit()).thenReturn(mockEditor)
        Mockito.lenient().`when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        Mockito.lenient().`when`(mockEditor.putInt(any(), any())).thenReturn(mockEditor)
        Mockito.lenient().`when`(mockEditor.commit()).thenReturn(true)
        Mockito.lenient().`when`(mockEditor.apply()).then {} // Mock apply() to do nothing
        
        Dispatchers.setMain(testDispatcher)
        
        // Reset the detection state before each test
        DetectionStateHolder.resetState()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `updateGoalMinutes updates goal minutes value`() = runTest {
        // Create ViewModel
        val viewModel = PracticeViewModel(mockApplication)
        
        // When
        viewModel.updateGoalMinutes(15)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(15, viewModel.goalMinutes.value)
        Mockito.verify(mockEditor).putInt("practice_goal_minutes", 15)
    }
} 