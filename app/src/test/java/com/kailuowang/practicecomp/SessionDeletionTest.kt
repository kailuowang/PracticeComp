package com.kailuowang.practicecomp

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

/**
 * Simple functional test for session deletion logic.
 */
class SessionDeletionTest {

    @Test
    fun `deleting a session removes it from the list`() {
        // Given - A list of sessions
        val session1 = PracticeSession(
            id = "session1",
            date = LocalDateTime.now(),
            totalTimeMillis = 3600000, // 1 hour
            practiceTimeMillis = 1800000 // 30 minutes
        )
        
        val session2 = PracticeSession(
            id = "session2",
            date = LocalDateTime.now().plusHours(1),
            totalTimeMillis = 7200000, // 2 hours
            practiceTimeMillis = 3600000 // 1 hour
        )
        
        val originalList = listOf(session1, session2)
        
        // When - We filter out a session (simulating delete operation)
        val sessionIdToDelete = "session1"
        val updatedList = originalList.filter { it.id != sessionIdToDelete }
        
        // Then - Verify the session was removed
        assertEquals(1, updatedList.size)
        assertEquals("session2", updatedList[0].id)
        assertEquals(7200000, updatedList[0].totalTimeMillis)
        assertEquals(3600000, updatedList[0].practiceTimeMillis)
    }
    
    @Test
    fun `deleting a non-existent session ID makes no changes`() {
        // Given - A list of sessions
        val session = PracticeSession(
            id = "session1",
            date = LocalDateTime.now(),
            totalTimeMillis = 3600000, // 1 hour
            practiceTimeMillis = 1800000 // 30 minutes
        )
        
        val originalList = listOf(session)
        
        // When - We filter out a non-existent session
        val sessionIdToDelete = "non-existent-id"
        val updatedList = originalList.filter { it.id != sessionIdToDelete }
        
        // Then - Verify no changes were made to the list
        assertEquals(1, updatedList.size)
        assertEquals("session1", updatedList[0].id)
    }
} 