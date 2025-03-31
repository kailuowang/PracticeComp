package com.kailuowang.practicecomp

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class PracticeSessionTest {

    @Test
    fun `getFormattedTotalTime formats time correctly with hours`() {
        // Given
        val session = PracticeSession(
            totalTimeMillis = 3723000L // 1h 2m 3s
        )
        
        // When
        val formattedTime = session.getFormattedTotalTime()
        
        // Then
        assertEquals("1:02:03", formattedTime)
    }
    
    @Test
    fun `getFormattedTotalTime formats time correctly without hours`() {
        // Given
        val session = PracticeSession(
            totalTimeMillis = 723000L // 12m 3s
        )
        
        // When
        val formattedTime = session.getFormattedTotalTime()
        
        // Then
        assertEquals("12:03", formattedTime)
    }

    @Test
    fun `getFormattedPracticeTime formats time correctly with hours`() {
        // Given
        val session = PracticeSession(
            practiceTimeMillis = 3723000L // 1h 2m 3s
        )
        
        // When
        val formattedTime = session.getFormattedPracticeTime()
        
        // Then
        assertEquals("1:02:03", formattedTime)
    }
    
    @Test
    fun `getFormattedPracticeTime formats time correctly without hours`() {
        // Given
        val session = PracticeSession(
            practiceTimeMillis = 723000L // 12m 3s
        )
        
        // When
        val formattedTime = session.getFormattedPracticeTime()
        
        // Then
        assertEquals("12:03", formattedTime)
    }
    
    @Test
    fun `getPracticePercentage calculates percentage correctly`() {
        // Given
        val session = PracticeSession(
            totalTimeMillis = 3600000L, // 1 hour
            practiceTimeMillis = 1800000L // 30 minutes (50%)
        )
        
        // When
        val percentage = session.getPracticePercentage()
        
        // Then
        assertEquals(50, percentage)
    }
    
    @Test
    fun `getPracticePercentage returns 0 when totalTimeMillis is 0`() {
        // Given
        val session = PracticeSession(
            totalTimeMillis = 0L,
            practiceTimeMillis = 1800000L 
        )
        
        // When
        val percentage = session.getPracticePercentage()
        
        // Then
        assertEquals(0, percentage)
    }
    
    @Test
    fun `getPracticePercentage handles 100 percent case`() {
        // Given
        val session = PracticeSession(
            totalTimeMillis = 1800000L, 
            practiceTimeMillis = 1800000L // 100%
        )
        
        // When
        val percentage = session.getPracticePercentage()
        
        // Then
        assertEquals(100, percentage)
    }
    
    @Test
    fun `getFormattedDate and getFormattedStartTime return expected format`() {
        // Given
        val fixedDateTime = LocalDateTime.of(2023, 5, 10, 14, 30)
        val session = PracticeSession(
            date = fixedDateTime
        )
        
        // When
        val formattedDate = session.getFormattedDate()
        val formattedTime = session.getFormattedStartTime()
        
        // Then - Using a more flexible assertion since formatting depends on locale
        val expectedDateFormat = fixedDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        val expectedTimeFormat = fixedDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        
        assertEquals(expectedDateFormat, formattedDate)
        assertEquals(expectedTimeFormat, formattedTime)
    }
} 