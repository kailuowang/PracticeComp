package com.kailuowang.practicecomp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the state related to practice detection, shared between Service and UI.
 */
data class DetectionState(
    val statusMessage: String = "Initializing...",
    val accumulatedTimeMillis: Long = 0L,
    val totalSessionTimeMillis: Long = 0L,
    val sessionStartTimeMillis: Long = System.currentTimeMillis()
)

object DetectionStateHolder {
    private val _state = MutableStateFlow(DetectionState())
    val state: StateFlow<DetectionState> = _state.asStateFlow()

    fun updateState(
        newStatus: String? = null, 
        newTimeMillis: Long? = null,
        newTotalSessionTimeMillis: Long? = null
    ) {
        _state.update { currentState ->
            currentState.copy(
                statusMessage = newStatus ?: currentState.statusMessage,
                accumulatedTimeMillis = newTimeMillis ?: currentState.accumulatedTimeMillis,
                totalSessionTimeMillis = newTotalSessionTimeMillis ?: currentState.totalSessionTimeMillis
            )
        }
    }

    // Optional: Reset function if needed when service starts/stops explicitly
    fun resetState() {
        _state.value = DetectionState()
    }

    // Keep the old updateStatus for simple status messages if convenient,
    // but mark as deprecated or encourage using updateState.
    @Deprecated("Use updateState for more complete updates", ReplaceWith("updateState(newStatus = status)"))
    fun updateStatus(status: String) {
        updateState(newStatus = status)
    }
} 