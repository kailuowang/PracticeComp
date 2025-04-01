package com.kailuowang.practicecomp

import org.junit.Test
import org.junit.Assert.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Unit tests for the MonthCalendar component's utility functions
 */
class MonthCalendarTest {

    @Test
    fun `first day of month index calculation is correct`() {
        // For this test, we'll recreate the logic from the MonthCalendar composable
        // to ensure it correctly determines the first day position
        
        // Let's test with a few known months
        // January 2023 - First day is Sunday (index 0 in US locale)
        testFirstDayIndex(YearMonth.of(2023, 1), Locale.US, 0)
        
        // February 2023 - First day is Wednesday (index 3 in US locale)
        testFirstDayIndex(YearMonth.of(2023, 2), Locale.US, 3)
        
        // March 2023 - First day is Wednesday (index 3 in US locale) 
        testFirstDayIndex(YearMonth.of(2023, 3), Locale.US, 3)
        
        // April 2023 - First day is Saturday (index 6 in US locale)
        testFirstDayIndex(YearMonth.of(2023, 4), Locale.US, 6)
        
        // Test with a different locale - UK (first day of week is Monday)
        // January 2023 - First day is Sunday (index 6 in UK locale)
        testFirstDayIndex(YearMonth.of(2023, 1), Locale.UK, 6)
        
        // February 2023 - First day is Wednesday (index 2 in UK locale)
        testFirstDayIndex(YearMonth.of(2023, 2), Locale.UK, 2)
    }
    
    private fun testFirstDayIndex(yearMonth: YearMonth, locale: Locale, expectedIndex: Int) {
        val firstDayOfMonth = yearMonth.atDay(1)
        val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
        
        // Calculate the day of week index for the first day of month (0-based, adjusted for locale)
        val firstDayOfMonthIndex = (firstDayOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        
        assertEquals("First day index for ${yearMonth.month} ${yearMonth.year} in ${locale.displayName} locale", 
                    expectedIndex, firstDayOfMonthIndex)
    }
    
    @Test
    fun `weeks in month calculation is correct`() {
        // Test with months of different lengths and starting days
        
        // January 2023 - 31 days, starts on Sunday (index 0 in US locale)
        // Requires 5 rows/weeks to display
        testWeeksInMonth(YearMonth.of(2023, 1), Locale.US, 5)
        
        // February 2023 - 28 days, starts on Wednesday (index 3 in US locale)
        // Requires 5 rows/weeks to display (4 complete weeks + 1 partial)
        testWeeksInMonth(YearMonth.of(2023, 2), Locale.US, 5)
        
        // July 2023 - 31 days, starts on Saturday (index 6 in US locale)
        // Requires 6 rows/weeks to display
        testWeeksInMonth(YearMonth.of(2023, 7), Locale.US, 6)
        
        // April 2023 - 30 days, starts on Saturday (index 6 in US locale)
        // Requires 6 rows/weeks to display
        testWeeksInMonth(YearMonth.of(2023, 4), Locale.US, 6)
    }
    
    private fun testWeeksInMonth(yearMonth: YearMonth, locale: Locale, expectedWeeks: Int) {
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()
        val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
        
        // Calculate the day of week index for the first day of month (0-based, adjusted for locale)
        val firstDayOfMonthIndex = (firstDayOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        
        // Calculate the number of weeks in the month view
        val daysInMonth = lastDayOfMonth.dayOfMonth
        val totalCells = firstDayOfMonthIndex + daysInMonth
        val weeksInMonth = (totalCells + 6) / 7 // Round up to full weeks
        
        assertEquals("Weeks needed for ${yearMonth.month} ${yearMonth.year} in ${locale.displayName} locale", 
                    expectedWeeks, weeksInMonth)
    }
    
    @Test
    fun `day of week ordering is correct based on locale`() {
        // Test US locale (Sunday first)
        val usDaysOfWeek = getOrderedDaysOfWeek(Locale.US)
        assertEquals(DayOfWeek.SUNDAY, usDaysOfWeek[0])
        assertEquals(DayOfWeek.MONDAY, usDaysOfWeek[1])
        assertEquals(DayOfWeek.SATURDAY, usDaysOfWeek[6])
        
        // Test UK locale (Monday first)
        val ukDaysOfWeek = getOrderedDaysOfWeek(Locale.UK)
        assertEquals(DayOfWeek.MONDAY, ukDaysOfWeek[0])
        assertEquals(DayOfWeek.TUESDAY, ukDaysOfWeek[1])
        assertEquals(DayOfWeek.SUNDAY, ukDaysOfWeek[6])
        
        // Test FRANCE locale (Monday first)
        val frDaysOfWeek = getOrderedDaysOfWeek(Locale.FRANCE)
        assertEquals(DayOfWeek.MONDAY, frDaysOfWeek[0])
        assertEquals(DayOfWeek.TUESDAY, frDaysOfWeek[1])
        assertEquals(DayOfWeek.SUNDAY, frDaysOfWeek[6])
    }
    
    private fun getOrderedDaysOfWeek(locale: Locale): List<DayOfWeek> {
        val daysOfWeek = DayOfWeek.values()
        val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
        
        return (0..6).map { i ->
            val index = (firstDayOfWeek.value - 1 + i) % 7
            daysOfWeek[index]
        }
    }
} 