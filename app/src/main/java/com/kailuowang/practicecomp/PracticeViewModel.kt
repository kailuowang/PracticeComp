package com.kailuowang.practicecomp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Represents the state for the instrument selection and active session
data class PracticeUiState(
    val availableInstruments: List<String> = listOf("Piano", "Cello", "Violin", "Clarinet"),
    val selectedInstrument: String? = null, // Instrument chosen in the current selection flow
    val defaultInstrument: String? = null   // Last confirmed instrument
)

class PracticeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        // Initialize selected instrument with the default when ViewModel is created
        // In a real app, you might load the default from SharedPreferences here
        _uiState.update { currentState ->
            currentState.copy(selectedInstrument = currentState.defaultInstrument)
        }
    }

    /**
     * Updates the temporarily selected instrument during the selection process.
     */
    fun selectInstrument(instrument: String) {
        _uiState.update { currentState ->
            currentState.copy(selectedInstrument = instrument)
        }
    }

    /**
     * Confirms the selected instrument, making it the default for the next session,
     * and potentially readying it for the active session screen.
     */
    fun confirmInstrumentSelection() {
        // The currently selected instrument becomes the new default
        val confirmedInstrument = _uiState.value.selectedInstrument
        _uiState.update { currentState ->
            currentState.copy(defaultInstrument = confirmedInstrument)
        }
        // In a real app, you would save the confirmedInstrument to SharedPreferences here
    }

    /**
     * Resets the selection state, e.g., when navigating away from selection screen
     * without confirming. Sets selection back to the last confirmed default.
     */
     fun resetSelectionToDefault() {
         _uiState.update { currentState ->
             currentState.copy(selectedInstrument = currentState.defaultInstrument)
         }
     }
} 