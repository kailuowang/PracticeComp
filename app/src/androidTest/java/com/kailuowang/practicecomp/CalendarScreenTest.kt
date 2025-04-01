package com.kailuowang.practicecomp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var mockViewModel: PracticeViewModel

    @Before
    fun setUp() {
        // Create mock ViewModel
        mockViewModel = Mockito.mock(PracticeViewModel::class.java)
        
        // Mock the necessary method calls on the ViewModel
        val currentMonth = YearMonth.now()
        val monthString = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        
        // Setup default responses
        whenever(mockViewModel.getPracticeDurationForMonth(currentMonth))
            .thenReturn(5400000) // 1h 30m in milliseconds
        whenever(mockViewModel.getLifetimePracticeDuration())
            .thenReturn(18000000) // 5h in milliseconds
        whenever(mockViewModel.formatPracticeDuration(5400000))
            .thenReturn("1h 30m")
        whenever(mockViewModel.formatPracticeDuration(18000000))
            .thenReturn("5h")
            
        // Create test sessions
        val today = LocalDateTime.now()
        val testSession = PracticeSession(
            date = today,
            totalTimeMillis = 3600000, // 1 hour
            practiceTimeMillis = 1800000 // 30 minutes
        )
        
        // Mock getPracticeDurationForDate for various dates
        whenever(mockViewModel.getPracticeDurationForDate(org.mockito.kotlin.any()))
            .thenReturn(0) // Default for most days
        whenever(mockViewModel.getPracticeDurationForDate(today.toLocalDate().atStartOfDay()))
            .thenReturn(1800000) // 30m for today
        whenever(mockViewModel.formatPracticeDuration(1800000))
            .thenReturn("30m")
    }

    @Test
    fun calendarScreenDisplaysCorrectTitle() {
        // Launch the CalendarScreen composable with the mock ViewModel
        composeTestRule.setContent {
            PracticeCalendarScreen(
                onBackClick = {},
                viewModel = mockViewModel
            )
        }
        
        // Verify the title is displayed
        composeTestRule.onNodeWithText("Practice Calendar").assertIsDisplayed()
    }
    
    @Test
    fun calendarScreenShowsCurrentMonth() {
        // Launch the CalendarScreen
        composeTestRule.setContent {
            PracticeCalendarScreen(
                onBackClick = {},
                viewModel = mockViewModel
            )
        }
        
        // Get the current month name and year
        val currentMonth = YearMonth.now()
        val monthString = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        
        // Verify the month is displayed
        composeTestRule.onNodeWithText(monthString).assertIsDisplayed()
    }
    
    @Test
    fun calendarScreenShowsPracticeStatistics() {
        // Launch the CalendarScreen
        composeTestRule.setContent {
            PracticeCalendarScreen(
                onBackClick = {},
                viewModel = mockViewModel
            )
        }
        
        // Verify the statistics section is displayed
        composeTestRule.onNodeWithText("Practice Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("This Month:").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lifetime Total:").assertIsDisplayed()
        
        // Verify the formatted durations
        composeTestRule.onNodeWithText("1h 30m").assertIsDisplayed()
        composeTestRule.onNodeWithText("5h").assertIsDisplayed()
    }
}


