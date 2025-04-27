package com.example.practicecompanion.data

import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a technical goal that a user can set for their practice sessions.
 */
data class TechnicalGoal(
    val id: String = UUID.randomUUID().toString(), // Use UUID for uniqueness
    val description: String,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val lastModifiedDate: LocalDateTime = LocalDateTime.now(), // For editing
    val achievedDate: LocalDateTime? = null,
    val isAchieved: Boolean = false
) 