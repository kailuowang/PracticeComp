package com.kailuowang.practicecomp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

// Data class to represent the UI state derived from DetectionStateHolder
data class PracticeUiState(
    val detectionStatus: String = "Initializing...",
    val formattedPracticeTime: String = "00:00:00",
    val formattedTotalSessionTime: String = "00:00:00",
    val sessionActive: Boolean = false
)

class PracticeViewModel : ViewModel() {

    // Store practice sessions
    private val _sessions = MutableStateFlow<List<PracticeSession>>(emptyList())
    val sessions: StateFlow<List<PracticeSession>> = _sessions.asStateFlow()

    // Observe the DetectionState from the Holder and map it to UI state
    val uiState: StateFlow<PracticeUiState> = DetectionStateHolder.state
        .map { detectionState ->
            val elapsedSessionTime = if (detectionState.sessionStartTimeMillis > 0) {
                System.currentTimeMillis() - detectionState.sessionStartTimeMillis
            } else {
                detectionState.totalSessionTimeMillis
            }
            
            PracticeUiState(
                detectionStatus = detectionState.statusMessage,
                formattedPracticeTime = formatMillisToTimer(detectionState.accumulatedTimeMillis),
                formattedTotalSessionTime = formatMillisToTimer(elapsedSessionTime),
                sessionActive = true
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep observing for 5s after last subscriber
            initialValue = PracticeUiState() // Initial UI state
        )

    // Helper function to format milliseconds into HH:MM:SS
    private fun formatMillisToTimer(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Save a completed practice session
    fun saveSession(totalTimeMillis: Long, practiceTimeMillis: Long) {
        val newSession = PracticeSession(
            date = LocalDateTime.now(),
            totalTimeMillis = totalTimeMillis,
            practiceTimeMillis = practiceTimeMillis
        )
        
        _sessions.update { currentSessions ->
            currentSessions + newSession
        }
    }
    
    // Update total session time (called regularly from the service)
    fun updateTotalSessionTime() {
        // This is primarily handled by the service through DetectionStateHolder,
        // but we could add additional logic here if needed
    }

    // TODO: Add methods to start/stop session, update timer, etc.

} 