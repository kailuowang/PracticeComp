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
import java.time.YearMonth

// Data class to represent the UI state derived from DetectionStateHolder
data class PracticeUiState(
    val detectionStatus: String = "Initializing...",
    val formattedPracticeTime: String = "00:00:00",
    val formattedTotalSessionTime: String = "00:00:00",
    val sessionActive: Boolean = false,
    val goalMinutes: Int = 0,
    val goalReached: Boolean = false
)

class PracticeViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("PracticeCompPrefs", Context.MODE_PRIVATE)
    private val PREF_KEY_SESSIONS = "saved_sessions"
    private val PREF_KEY_GOAL_MINUTES = "practice_goal_minutes"
    
    // Store practice sessions
    private val _sessions = MutableStateFlow<List<PracticeSession>>(emptyList())
    val sessions: StateFlow<List<PracticeSession>> = _sessions.asStateFlow()
    
    // Session goal in minutes
    private val _goalMinutes = MutableStateFlow(sharedPrefs.getInt(PREF_KEY_GOAL_MINUTES, 0))
    val goalMinutes: StateFlow<Int> = _goalMinutes.asStateFlow()
    
    // Flag to track if goal has been announced
    private val _goalAnnounced = MutableStateFlow(false)

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
            
            val currentGoalMinutes = _goalMinutes.value
            val practiceTimeMillis = detectionState.accumulatedTimeMillis
            val goalReached = currentGoalMinutes > 0 && 
                practiceTimeMillis >= (currentGoalMinutes * 60 * 1000)
                
            // Update goal announced flag if needed
            if (goalReached && !_goalAnnounced.value) {
                _goalAnnounced.value = true
            }
            
            PracticeUiState(
                detectionStatus = detectionState.statusMessage,
                formattedPracticeTime = formatMillisToTimer(detectionState.accumulatedTimeMillis),
                formattedTotalSessionTime = formatMillisToTimer(elapsedSessionTime),
                sessionActive = true,
                goalMinutes = currentGoalMinutes,
                goalReached = goalReached
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep observing for 5s after last subscriber
            initialValue = PracticeUiState(goalMinutes = _goalMinutes.value) // Initial UI state
        )

    // Update practice goal in minutes
    fun updateGoalMinutes(minutes: Int) {
        if (minutes != _goalMinutes.value) {
            _goalMinutes.value = minutes
            _goalAnnounced.value = false // Reset announcement flag for new goal
            
            // Save to SharedPreferences
            sharedPrefs.edit().putInt(PREF_KEY_GOAL_MINUTES, minutes).apply()
            
            Log.d("PracticeViewModel", "Updated practice goal to $minutes minutes")
        }
    }
    
    // Check if goal is reached but not yet announced
    fun isGoalJustReached(): Boolean {
        val currentState = uiState.value
        
        // Check if goal is reached according to current UI state
        if (currentState.goalReached && !_goalAnnounced.value) {
            // Set the flag to true
            _goalAnnounced.value = true
            return true
        }
        return false
    }
    
    // Reset goal announcement flag (e.g., for new sessions)
    fun resetGoalAnnounced() {
        _goalAnnounced.value = false
    }

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
        if (totalTimeMillis <= 0) {
            Log.w("PracticeViewModel", "Attempted to save session with zero or negative total time, ignoring")
            return
        }
        
        val newSession = PracticeSession(
            date = LocalDateTime.now(),
            totalTimeMillis = totalTimeMillis,
            practiceTimeMillis = practiceTimeMillis
        )
        
        Log.d("PracticeViewModel", "Saving session - Total: ${newSession.getFormattedTotalTime()}, Practice: ${newSession.getFormattedPracticeTime()}")
        
        // Save immediately to both memory and SharedPreferences
        saveSessionImmediate(newSession)
    }
    
    // Synchronously save session to both memory and SharedPreferences
    private fun saveSessionImmediate(newSession: PracticeSession) {
        try {
            // Update in-memory list
            val currentSessions = _sessions.value
            val updatedSessions = listOf(newSession) + currentSessions
            _sessions.value = updatedSessions
            
            // Convert to JSON for SharedPreferences
            val jsonArray = JSONArray()
            updatedSessions.forEach { session ->
                val jsonObject = sessionToJson(session)
                jsonArray.put(jsonObject)
            }
            
            // Get editor and commit changes synchronously
            val editor = sharedPrefs.edit()
            editor.putString(PREF_KEY_SESSIONS, jsonArray.toString())
            val success = editor.commit() // Use commit for immediate synchronous saving
            
            Log.d("PracticeViewModel", "Session saved - Success: $success, Total sessions: ${updatedSessions.size}")
        } catch (e: Exception) {
            Log.e("PracticeViewModel", "Error saving session", e)
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
                    
                    Log.d("PracticeViewModel", "Loaded ${loadedSessions.size} saved sessions")
                    
                    withContext(Dispatchers.Main) {
                        _sessions.value = loadedSessions
                    }
                } catch (e: Exception) {
                    Log.e("PracticeViewModel", "Error loading saved sessions", e)
                }
            } else {
                Log.d("PracticeViewModel", "No saved sessions found")
            }
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

    // Public method to refresh sessions from SharedPreferences
    fun refreshSessions() {
        Log.d("PracticeViewModel", "Refreshing sessions from SharedPreferences")
        try {
            // Load synchronously on the main thread to ensure immediate UI update
            val sessionsJson = sharedPrefs.getString(PREF_KEY_SESSIONS, null)
            Log.d("PracticeViewModel", "Raw JSON from SharedPreferences: ${sessionsJson?.take(100)}...")
            
            if (!sessionsJson.isNullOrEmpty()) {
                val loadedSessions = mutableListOf<PracticeSession>()
                try {
                    val jsonArray = JSONArray(sessionsJson)
                    Log.d("PracticeViewModel", "JSON array length: ${jsonArray.length()}")
                    
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val session = jsonToSession(jsonObject)
                        loadedSessions.add(session)
                    }
                    
                    // Sort sessions by date in reverse order (newest first)
                    val sortedSessions = loadedSessions.sortedByDescending { it.date }
                    Log.d("PracticeViewModel", "Loaded ${sortedSessions.size} saved sessions")
                    
                    // Update the state
                    _sessions.value = sortedSessions
                } catch (e: Exception) {
                    Log.e("PracticeViewModel", "Error parsing session JSON", e)
                }
            } else {
                Log.d("PracticeViewModel", "No saved sessions found in SharedPreferences")
            }
        } catch (e: Exception) {
            Log.e("PracticeViewModel", "Error refreshing sessions", e)
        }
    }

    // Delete a session by ID and update SharedPreferences
    fun deleteSession(sessionId: String) {
        try {
            Log.d("PracticeViewModel", "Deleting session with ID: $sessionId")
            
            // Create a new list without the session to delete
            val currentSessions = _sessions.value
            val updatedSessions = currentSessions.filter { it.id != sessionId }
            
            // Update in-memory list
            _sessions.value = updatedSessions
            
            // Save to SharedPreferences
            val jsonArray = JSONArray()
            updatedSessions.forEach { session ->
                val jsonObject = sessionToJson(session)
                jsonArray.put(jsonObject)
            }
            
            // Get editor and commit changes synchronously
            val editor = sharedPrefs.edit()
            editor.putString(PREF_KEY_SESSIONS, jsonArray.toString())
            val success = editor.commit() // Use commit for immediate synchronous saving
            
            Log.d("PracticeViewModel", "Session deleted - Success: $success, Remaining sessions: ${updatedSessions.size}")
        } catch (e: Exception) {
            Log.e("PracticeViewModel", "Error deleting session", e)
        }
    }

    // Returns the practice time for a specific date in milliseconds
    fun getPracticeDurationForDate(date: LocalDateTime): Long {
        val dateString = date.toLocalDate().toString()
        
        // Log the date range and sessions being checked for debugging
        Log.d("PracticeViewModel", "Checking for sessions on $dateString, total sessions: ${_sessions.value.size}")
        
        // Use date comparison on just the date part, not the full timestamp
        return _sessions.value
            .filter { 
                it.date.toLocalDate() == date.toLocalDate()
            }
            .also { matchedSessions ->
                if (matchedSessions.isNotEmpty()) {
                    Log.d("PracticeViewModel", "Found ${matchedSessions.size} sessions for $dateString")
                    matchedSessions.forEach {
                        Log.d("PracticeViewModel", "Matched session: ${it.date}, practice time: ${it.practiceTimeMillis}, total time: ${it.totalTimeMillis}")
                    }
                }
            }
            .sumOf { it.practiceTimeMillis }
    }
    
    // Returns the practice time for an entire month in milliseconds
    fun getPracticeDurationForMonth(yearMonth: YearMonth): Long {
        val startOfMonth = yearMonth.atDay(1).atStartOfDay()
        val endOfMonth = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusNanos(1)
        
        Log.d("PracticeViewModel", "Checking for sessions in month: $yearMonth")
        
        return _sessions.value
            .filter { 
                val sessionYearMonth = YearMonth.from(it.date)
                sessionYearMonth == yearMonth
            }
            .also { matchedSessions ->
                Log.d("PracticeViewModel", "Found ${matchedSessions.size} sessions for month $yearMonth")
                matchedSessions.forEach {
                    Log.d("PracticeViewModel", "Matched session: ${it.date}, practice time: ${it.practiceTimeMillis}")
                }
            }
            .sumOf { it.practiceTimeMillis }
    }
    
    // Returns the total practice time across all sessions in milliseconds
    fun getLifetimePracticeDuration(): Long {
        return _sessions.value.sumOf { it.practiceTimeMillis }
    }
    
    // Formats a duration in milliseconds to a human-readable string (e.g., "2h 15m")
    fun formatPracticeDuration(durationMillis: Long): String {
        // Calculate total hours (including any days)
        val totalHours = durationMillis / (1000 * 60 * 60)
        val minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (durationMillis % (1000 * 60)) / 1000
        
        return when {
            // Show hours and minutes when hours are present (even if minutes = 0)
            totalHours > 0 -> "${totalHours}h ${minutes}m"
            
            // Just minutes when between 1-59 minutes
            minutes > 0 -> "${minutes}m"
            
            // Seconds when less than a minute
            seconds > 0 -> "${seconds}s"
            
            // Zero case
            else -> "0s"
        }
    }

    // TODO: Add methods to start/stop session, update timer, etc.

} 