package com.kailuowang.practicecomp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Data class to represent the UI state derived from DetectionStateHolder
data class PracticeUiState(
    val detectionStatus: String = "Initializing...",
    val formattedPracticeTime: String = "00:00:00"
)

class PracticeViewModel : ViewModel() {

    // Observe the DetectionState from the Holder and map it to UI state
    val uiState: StateFlow<PracticeUiState> = DetectionStateHolder.state
        .map { detectionState ->
            PracticeUiState(
                detectionStatus = detectionState.statusMessage,
                formattedPracticeTime = formatMillisToTimer(detectionState.accumulatedTimeMillis)
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

    // TODO: Add methods to start/stop session, update timer, etc.

} 