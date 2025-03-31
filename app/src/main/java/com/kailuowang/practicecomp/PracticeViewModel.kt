package com.kailuowang.practicecomp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Represents the state for the active session
data class PracticeUiState(
    val isSessionActive: Boolean = false,
    val detectionStatus: String = "Idle"
)

class PracticeViewModel : ViewModel() {

    // Combine detection status into the main UI state
    val uiState: StateFlow<PracticeUiState> = DetectionStateHolder.detectionStatus
        .map { status ->
            PracticeUiState(detectionStatus = status)
            // TODO: Combine with other state like isSessionActive when needed
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PracticeUiState() // Initial state
        )

    // TODO: Add methods to start/stop session, update timer, etc.

} 