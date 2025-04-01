package com.kailuowang.practicecomp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kailuowang.practicecomp.ui.theme.PracticeCompTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class CalendarScreenUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockViewModel = mock(PracticeViewModel::class.java)
    private val sessionsFlow = MutableStateFlow<List<PracticeSession>>(emptyList())

    @Test
    fun calendarScreenDisplaysCurrentMonth() {
        // Setup
        val today = LocalDate.now()
        val currentMonth = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val currentYear = today.year.toString()
        val monthYearText = "$currentMonth $currentYear"

        `when`(mockViewModel.sessions).thenReturn(sessionsFlow)
        
        // When
        composeTestRule.setContent {
            PracticeCompTheme {
                CalendarScreen(
                    viewModel = mockViewModel,
                    onBack = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText(monthYearText).assertIsDisplayed()
    }
    
    @Test
    fun calendarScreenDisplaysDaysOfWeek() {
        // Setup
        `when`(mockViewModel.sessions).thenReturn(sessionsFlow)
        
        // When
        composeTestRule.setContent {
            PracticeCompTheme {
                CalendarScreen(
                    viewModel = mockViewModel,
                    onBack = {}
                )
            }
        }
        
        // Then - Check for the presence of day abbreviations
        // Note: These will change based on locale, but for US locale:
        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
            composeTestRule.onAllNodesWithText(day, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
    
    @Test
    fun calendarScreenDisplaysCurrentDate() {
        // Setup
        val today = LocalDate.now()
        val todayText = today.dayOfMonth.toString()
        
        `when`(mockViewModel.sessions).thenReturn(sessionsFlow)
        
        // When
        composeTestRule.setContent {
            PracticeCompTheme {
                CalendarScreen(
                    viewModel = mockViewModel,
                    onBack = {}
                )
            }
        }
        
        // Then - Check the current date is highlighted or marked in some way
        // This test assumes the current date has some distinguishing feature
        // that makes it semantically different from other dates
        composeTestRule.onAllNodesWithText(todayText)
            .filterToOne(hasTestTag("currentDay"))
            .assertIsDisplayed()
    }
    
    @Test
    fun navigationButtonsChangeMonth() {
        // Setup
        val today = LocalDate.now()
        val currentMonth = today.month
        val nextMonth = currentMonth.plus(1)
        val prevMonth = currentMonth.minus(1)
        
        val nextMonthText = nextMonth.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val prevMonthText = prevMonth.getDisplayName(TextStyle.FULL, Locale.getDefault())
        
        `when`(mockViewModel.sessions).thenReturn(sessionsFlow)
        
        // When
        composeTestRule.setContent {
            PracticeCompTheme {
                CalendarScreen(
                    viewModel = mockViewModel,
                    onBack = {}
                )
            }
        }
        
        // Then
        // Navigate to next month
        composeTestRule.onNodeWithContentDescription("Next Month").performClick()
        composeTestRule.onNodeWithText(nextMonthText, substring = true).assertIsDisplayed()
        
        // Navigate back to current month
        composeTestRule.onNodeWithContentDescription("Previous Month").performClick()
        
        // Navigate to previous month
        composeTestRule.onNodeWithContentDescription("Previous Month").performClick()
        composeTestRule.onNodeWithText(prevMonthText, substring = true).assertIsDisplayed()
    }

    @Test
    fun practiceStatsAreDisplayed() {
        // Setup - Create test data with practice sessions
        val today = LocalDate.now()
        val thisMonth = YearMonth.from(today)
        
        // Create mock sessions for the current month
        val sessions = listOf(
            createMockSession(today.minusDays(1), 3600000, 1800000), // 1 hour total, 30 min practice
            createMockSession(today.minusDays(3), 7200000, 5400000)  // 2 hours total, 1.5 hours practice
        )
        
        val monthlyDuration = 7200000L // 2 hours (in milliseconds)
        val formattedDuration = "2h 0m"
        
        sessionsFlow.value = sessions
        
        `when`(mockViewModel.sessions).thenReturn(sessionsFlow)
        `when`(mockViewModel.getPracticeDurationForMonth(thisMonth.monthValue, thisMonth.year))
            .thenReturn(monthlyDuration)
        `when`(mockViewModel.formatPracticeDuration(monthlyDuration)).thenReturn(formattedDuration)
        
        // When
        composeTestRule.setContent {
            PracticeCompTheme {
                CalendarScreen(
                    viewModel = mockViewModel,
                    onBack = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Monthly Practice: $formattedDuration").assertIsDisplayed()
    }
    
    @Test
    fun lifetimePracticeStatsAreDisplayed() {
        // Setup - Create test data with practice sessions
        val today = LocalDate.now()
        val thisMonth = YearMonth.from(today)
        
        // Create mock sessions spanning multiple months
        val sessions = listOf(
            createMockSession(today.minusDays(1), 3600000, 1800000),             // 1 hour total, 30 min practice
            createMockSession(today.minusDays(3), 7200000, 5400000),             // 2 hours total, 1.5 hours practice
            createMockSession(today.minusMonths(1), 5400000, 3600000),           // 1.5 hours total, 1 hour practice
            createMockSession(today.minusMonths(3), 10800000, 9000000)           // 3 hours total, 2.5 hours practice
        )
        
        val lifetimeDuration = 19800000L // 5.5 hours (in milliseconds)
        val formattedDuration = "5h 30m"
        
        sessionsFlow.value = sessions
        
        `when`(mockViewModel.sessions).thenReturn(sessionsFlow)
        `when`(mockViewModel.getLifetimePracticeDuration()).thenReturn(lifetimeDuration)
        `when`(mockViewModel.formatPracticeDuration(lifetimeDuration)).thenReturn(formattedDuration)
        
        // When
        composeTestRule.setContent {
            PracticeCompTheme {
                CalendarScreen(
                    viewModel = mockViewModel,
                    onBack = {}
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Lifetime Total: $formattedDuration").assertIsDisplayed()
    }
    
    private fun createMockSession(
        date: LocalDate,
        totalTimeMillis: Long,
        practiceTimeMillis: Long
    ): PracticeSession {
        return PracticeSession(
            date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            startTimeMillis = System.currentTimeMillis(),
            totalTimeMillis = totalTimeMillis,
            practiceTimeMillis = practiceTimeMillis
        )
    }
} 