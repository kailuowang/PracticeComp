package com.kailuowang.practicecomp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple singleton to hold and share the music detection status between the
 * Service and the UI (via ViewModel).
 */
object DetectionStateHolder {
    private val _detectionStatus = MutableStateFlow("Idle")
    val detectionStatus = _detectionStatus.asStateFlow()

    fun updateStatus(newStatus: String) {
        _detectionStatus.value = newStatus
    }
} 