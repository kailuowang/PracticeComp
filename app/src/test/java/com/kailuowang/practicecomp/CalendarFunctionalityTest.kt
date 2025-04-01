package com.kailuowang.practicecomp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.lang.reflect.Field
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * Unit tests for the calendar-related functionality in PracticeViewModel
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CalendarFunctionalityTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PracticeViewModel
    
    @Mock
    private lateinit var mockApplication: Application
    
    @Mock
    private lateinit var mockSharedPrefs: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private fun injectTestSessions(sessions: List<PracticeSession>) {
        try {
            val field = PracticeViewModel::class.java.getDeclaredField("_sessions")
            field.isAccessible = true
            field.set(viewModel, MutableStateFlow(sessions))
        } catch (e: Exception) {
            throw RuntimeException("Failed to set test sessions", e)
        }
    }

    @Before
    fun setup() {
        // Mock SharedPreferences
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
    fun `getPracticeDurationForDate returns correct sum for sessions on a specific date`() = runTest {
        // Create three sessions: two on the same date and one on a different date
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        
        val session1 = PracticeSession(
            date = today.atTime(10, 0), // Today at 10:00 AM
            totalTimeMillis = 3600000, // 1 hour
            practiceTimeMillis = 1800000 // 30 minutes
        )
        
        val session2 = PracticeSession(
            date = today.atTime(15, 0), // Today at 3:00 PM
            totalTimeMillis = 1800000, // 30 minutes
            practiceTimeMillis = 900000 // 15 minutes
        )
        
        val session3 = PracticeSession(
            date = yesterday.atTime(14, 0), // Yesterday at 2:00 PM
            totalTimeMillis = 7200000, // 2 hours
            practiceTimeMillis = 3600000 // 1 hour
        )
        
        val testSessions = listOf(session1, session2, session3)
        injectTestSessions(testSessions)
        
        // Test for today's sessions
        val todayDuration = viewModel.getPracticeDurationForDate(today.atStartOfDay())
        assertEquals(2700000, todayDuration) // Sum of 30 min + 15 min = 45 min = 2,700,000 ms
        
        // Test for yesterday's session
        val yesterdayDuration = viewModel.getPracticeDurationForDate(yesterday.atStartOfDay())
        assertEquals(3600000, yesterdayDuration) // 1 hour = 3,600,000 ms
        
        // Test for a date with no sessions
        val twoDaysAgo = today.minusDays(2)
        val twoDaysAgoDuration = viewModel.getPracticeDurationForDate(twoDaysAgo.atStartOfDay())
        assertEquals(0, twoDaysAgoDuration)
    }
    
    @Test
    fun `getPracticeDurationForMonth returns correct sum for sessions in a specific month`() = runTest {
        // Create sessions in different months
        val thisMonth = YearMonth.now()
        val lastMonth = thisMonth.minusMonths(1)
        
        val session1 = PracticeSession(
            date = thisMonth.atDay(5).atTime(10, 0),
            totalTimeMillis = 3600000, // 1 hour
            practiceTimeMillis = 1800000 // 30 minutes
        )
        
        val session2 = PracticeSession(
            date = thisMonth.atDay(10).atTime(15, 0),
            totalTimeMillis = 1800000, // 30 minutes
            practiceTimeMillis = 900000 // 15 minutes
        )
        
        val session3 = PracticeSession(
            date = lastMonth.atDay(15).atTime(14, 0),
            totalTimeMillis = 7200000, // 2 hours
            practiceTimeMillis = 3600000 // 1 hour
        )
        
        val testSessions = listOf(session1, session2, session3)
        injectTestSessions(testSessions)
        
        // Test for this month's sessions
        val thisMonthDuration = viewModel.getPracticeDurationForMonth(thisMonth)
        assertEquals(2700000, thisMonthDuration) // Sum of 30 min + 15 min = 45 min = 2,700,000 ms
        
        // Test for last month's session
        val lastMonthDuration = viewModel.getPracticeDurationForMonth(lastMonth)
        assertEquals(3600000, lastMonthDuration) // 1 hour = 3,600,000 ms
        
        // Test for a month with no sessions
        val twoMonthsAgo = thisMonth.minusMonths(2)
        val twoMonthsAgoDuration = viewModel.getPracticeDurationForMonth(twoMonthsAgo)
        assertEquals(0, twoMonthsAgoDuration)
    }
    
    @Test
    fun `getLifetimePracticeDuration returns the sum of all sessions`() = runTest {
        val session1 = PracticeSession(
            date = LocalDateTime.now().minusDays(10),
            totalTimeMillis = 3600000, // 1 hour
            practiceTimeMillis = 1800000 // 30 minutes
        )
        
        val session2 = PracticeSession(
            date = LocalDateTime.now().minusDays(5),
            totalTimeMillis = 1800000, // 30 minutes
            practiceTimeMillis = 900000 // 15 minutes
        )
        
        val session3 = PracticeSession(
            date = LocalDateTime.now().minusDays(1),
            totalTimeMillis = 7200000, // 2 hours
            practiceTimeMillis = 3600000 // 1 hour
        )
        
        val testSessions = listOf(session1, session2, session3)
        injectTestSessions(testSessions)
        
        // Test lifetime duration
        val lifetimeDuration = viewModel.getLifetimePracticeDuration()
        assertEquals(6300000, lifetimeDuration) // Sum of all practice times = 1 hour 45 min = 6,300,000 ms
    }
    
    @Test
    fun `formatPracticeDuration formats durations correctly`() {
        // Test with days, hours and minutes
        assertEquals("2d 5h 30m", viewModel.formatPracticeDuration(2 * 24 * 60 * 60 * 1000 + 5 * 60 * 60 * 1000 + 30 * 60 * 1000)) // 2d 5h 30m
        
        // Test with hours and minutes
        assertEquals("2h 30m", viewModel.formatPracticeDuration(9000000)) // 2h 30m
        
        // Test with hours and zero minutes
        assertEquals("1h 0m", viewModel.formatPracticeDuration(3600000)) // 1h 0m
        
        // Test with only minutes
        assertEquals("30m", viewModel.formatPracticeDuration(1800000)) // 30m
        
        // Test with 0 duration
        assertEquals("0s", viewModel.formatPracticeDuration(0))
        
        // Test with small duration (less than 1 minute)
        assertEquals("30s", viewModel.formatPracticeDuration(30000)) // 30 seconds
        
        // Test with very small duration
        assertEquals("5s", viewModel.formatPracticeDuration(5000)) // 5 seconds
    }
    
    @Test
    fun `getPracticeDurationForDate handles timezone edge cases correctly`() = runTest {
        // Test edge cases with timestamps close to midnight
        val today = LocalDate.now()
        
        // Create a session just before midnight
        val lateNightSession = PracticeSession(
            date = today.atTime(23, 59, 30), // Today at 11:59:30 PM
            totalTimeMillis = 1800000, // 30 minutes
            practiceTimeMillis = 1200000 // 20 minutes
        )
        
        // Create a session just after midnight
        val earlyMorningSession = PracticeSession(
            date = today.plusDays(1).atTime(0, 0, 30), // Tomorrow at 12:00:30 AM
            totalTimeMillis = 1200000, // 20 minutes
            practiceTimeMillis = 600000 // 10 minutes
        )
        
        val testSessions = listOf(lateNightSession, earlyMorningSession)
        injectTestSessions(testSessions)
        
        // Test for today's sessions (should only include the late night session)
        val todayDuration = viewModel.getPracticeDurationForDate(today.atStartOfDay())
        assertEquals(1200000, todayDuration) // 20 minutes = 1,200,000 ms
        
        // Test for tomorrow's sessions (should only include the early morning session)
        val tomorrowDuration = viewModel.getPracticeDurationForDate(today.plusDays(1).atStartOfDay())
        assertEquals(600000, tomorrowDuration) // 10 minutes = 600,000 ms
    }
    
    @Test
    fun `getPracticeDurationForMonth handles month boundaries correctly`() = runTest {
        // Test edge cases with dates near month boundaries
        val thisMonth = YearMonth.now()
        val nextMonth = thisMonth.plusMonths(1)
        
        // Create a session on the last day of this month
        val lastDaySession = PracticeSession(
            date = thisMonth.atEndOfMonth().atTime(23, 0),
            totalTimeMillis = 3600000, // 1 hour
            practiceTimeMillis = 2400000 // 40 minutes
        )
        
        // Create a session on the first day of next month
        val firstDayNextMonthSession = PracticeSession(
            date = nextMonth.atDay(1).atTime(1, 0),
            totalTimeMillis = 1800000, // 30 minutes
            practiceTimeMillis = 1200000 // 20 minutes
        )
        
        val testSessions = listOf(lastDaySession, firstDayNextMonthSession)
        injectTestSessions(testSessions)
        
        // Test for this month (should only include the last day session)
        val thisMonthDuration = viewModel.getPracticeDurationForMonth(thisMonth)
        assertEquals(2400000, thisMonthDuration) // 40 minutes = 2,400,000 ms
        
        // Test for next month (should only include the first day session)
        val nextMonthDuration = viewModel.getPracticeDurationForMonth(nextMonth)
        assertEquals(1200000, nextMonthDuration) // 20 minutes = 1,200,000 ms
    }
} 