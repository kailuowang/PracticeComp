package com.kailuowang.practicecomp

/**
 * Simple interface for providing the current time, allowing for dependency injection in tests.
 */
interface Clock {
    fun getCurrentTimeMillis(): Long
}

/**
 * Default implementation using the system clock.
 */
object SystemClock : Clock {
    override fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
} 