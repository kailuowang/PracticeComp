package com.kailuowang.practicecomp

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Data class to represent the UI state derived from DetectionStateHolder
data class PracticeUiState(
    val detectionStatus: String = "Initializing...",
    val formattedPracticeTime: String = "00:00:00",
    val formattedTotalSessionTime: String = "00:00:00",
    val sessionActive: Boolean = false
)

class PracticeViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("PracticeCompPrefs", Context.MODE_PRIVATE)
    private val PREF_KEY_SESSIONS = "saved_sessions"
    
    // Store practice sessions
    private val _sessions = MutableStateFlow<List<PracticeSession>>(emptyList())
    val sessions: StateFlow<List<PracticeSession>> = _sessions.asStateFlow()

    init {
        // Load saved sessions when ViewModel is created
        loadSavedSessions()
    }
    
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
        
        Log.d("PracticeViewModel", "Saving session - Total: ${newSession.getFormattedTotalTime()}, Practice: ${newSession.getFormattedPracticeTime()}")
        
        _sessions.update { currentSessions ->
            // Add new session to the beginning of the list (newest first)
            val updatedSessions = listOf(newSession) + currentSessions
            Log.d("PracticeViewModel", "Session saved - Current sessions count: ${updatedSessions.size}")
            
            // Save to SharedPreferences in a background thread
            viewModelScope.launch(Dispatchers.IO) {
                saveSessionsToPrefs(updatedSessions)
            }
            
            updatedSessions
        }
    }
    
    private fun loadSavedSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            val sessionsJson = sharedPrefs.getString(PREF_KEY_SESSIONS, null)
            val loadedSessions = mutableListOf<PracticeSession>()
            
            if (!sessionsJson.isNullOrEmpty()) {
                try {
                    val jsonArray = JSONArray(sessionsJson)
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val session = jsonToSession(jsonObject)
                        loadedSessions.add(session)
                    }
                    
                    // Sort sessions by date in reverse order (newest first)
                    val sortedSessions = loadedSessions.sortedByDescending { it.date }
                    
                    Log.d("PracticeViewModel", "Loaded ${sortedSessions.size} saved sessions")
                    
                    withContext(Dispatchers.Main) {
                        _sessions.value = sortedSessions
                    }
                } catch (e: Exception) {
                    Log.e("PracticeViewModel", "Error loading saved sessions", e)
                }
            } else {
                Log.d("PracticeViewModel", "No saved sessions found")
            }
        }
    }
    
    private fun saveSessionsToPrefs(sessionsList: List<PracticeSession>) {
        try {
            val jsonArray = JSONArray()
            sessionsList.forEach { session ->
                val jsonObject = sessionToJson(session)
                jsonArray.put(jsonObject)
            }
            
            sharedPrefs.edit()
                .putString(PREF_KEY_SESSIONS, jsonArray.toString())
                .apply()
            
            Log.d("PracticeViewModel", "Saved ${sessionsList.size} sessions to SharedPreferences")
        } catch (e: Exception) {
            Log.e("PracticeViewModel", "Error saving sessions to SharedPreferences", e)
        }
    }
    
    private fun sessionToJson(session: PracticeSession): JSONObject {
        return JSONObject().apply {
            put("id", session.id)
            put("date", session.date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            put("totalTimeMillis", session.totalTimeMillis)
            put("practiceTimeMillis", session.practiceTimeMillis)
        }
    }
    
    private fun jsonToSession(json: JSONObject): PracticeSession {
        val id = json.getString("id")
        val dateString = json.getString("date")
        val date = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val totalTimeMillis = json.getLong("totalTimeMillis")
        val practiceTimeMillis = json.getLong("practiceTimeMillis")
        
        return PracticeSession(
            id = id,
            date = date,
            totalTimeMillis = totalTimeMillis,
            practiceTimeMillis = practiceTimeMillis
        )
    }
    
    // Update total session time (called regularly from the service)
    fun updateTotalSessionTime() {
        // This is primarily handled by the service through DetectionStateHolder,
        // but we could add additional logic here if needed
    }

    // TODO: Add methods to start/stop session, update timer, etc.

} 