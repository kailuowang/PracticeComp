package com.kailuowang.practicecomp

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Represents a completed practice session
 */
data class PracticeSession(
    val id: String = System.currentTimeMillis().toString(),
    val date: LocalDateTime = LocalDateTime.now(),
    val totalTimeMillis: Long = 0,
    val practiceTimeMillis: Long = 0,
    val targetedGoalIds: List<String> = emptyList() // IDs of technical goals targeted in this session
) {
    
    // Display formatting methods
    fun getFormattedDate(): String {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val sessionDate = date.toLocalDate()
        
        return when (sessionDate) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }
    }
    
    fun getFormattedStartTime(): String {
        return date.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
    
    fun getFormattedTotalTime(): String {
        val hours = totalTimeMillis / (1000 * 60 * 60)
        val minutes = (totalTimeMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (totalTimeMillis % (1000 * 60)) / 1000
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    fun getFormattedPracticeTime(): String {
        val hours = practiceTimeMillis / (1000 * 60 * 60)
        val minutes = (practiceTimeMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (practiceTimeMillis % (1000 * 60)) / 1000
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    fun getPracticePercentage(): Int {
        if (totalTimeMillis <= 0) return 0
        return ((practiceTimeMillis.toDouble() / totalTimeMillis.toDouble()) * 100).toInt()
    }
} 