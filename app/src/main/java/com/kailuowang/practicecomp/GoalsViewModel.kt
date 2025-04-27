package com.kailuowang.practicecomp

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * ViewModel for managing technical goals.
 */
class GoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("PracticeCompPrefs", Context.MODE_PRIVATE)
    private val PREF_KEY_GOALS = "technical_goals"
    
    // Store all technical goals
    private val _allGoals = MutableStateFlow<List<TechnicalGoal>>(emptyList())
    val allGoals: StateFlow<List<TechnicalGoal>> = _allGoals.asStateFlow()
    
    // Derived flow for outstanding (not achieved) goals
    val outstandingGoals = _allGoals.map { goals ->
        goals.filter { !it.isAchieved }
    }
    
    // Selected goals for current session
    private val _sessionTargetedGoals = MutableStateFlow<List<TechnicalGoal>>(emptyList())
    val sessionTargetedGoals: StateFlow<List<TechnicalGoal>> = _sessionTargetedGoals.asStateFlow()

    init {
        // Load saved goals when ViewModel is created
        loadSavedGoals()
    }
    
    /**
     * Creates a new technical goal.
     */
    fun createGoal(description: String) {
        if (description.isBlank()) {
            Log.w("GoalsViewModel", "Attempted to create goal with empty description, ignoring")
            return
        }
        
        val newGoal = TechnicalGoal(
            description = description
        )
        
        Log.d("GoalsViewModel", "Creating new goal: ${newGoal.description}")
        
        // Update list and save to SharedPreferences
        val updatedGoals = _allGoals.value + newGoal
        _allGoals.value = updatedGoals
        saveGoalsToPreferences(updatedGoals)
    }
    
    /**
     * Updates an existing goal's description.
     */
    fun updateGoalDescription(goalId: String, newDescription: String) {
        if (newDescription.isBlank()) {
            Log.w("GoalsViewModel", "Attempted to update goal with empty description, ignoring")
            return
        }
        
        val currentGoals = _allGoals.value
        val updatedGoals = currentGoals.map { goal ->
            if (goal.id == goalId) {
                goal.copy(
                    description = newDescription,
                    lastModifiedDate = LocalDateTime.now()
                )
            } else {
                goal
            }
        }
        
        _allGoals.value = updatedGoals
        saveGoalsToPreferences(updatedGoals)
        
        Log.d("GoalsViewModel", "Updated goal description for goal ID: $goalId")
    }
    
    /**
     * Toggles the achievement status of a goal.
     */
    fun toggleGoalAchievement(goalId: String, isAchieved: Boolean) {
        val currentGoals = _allGoals.value
        val updatedGoals = currentGoals.map { goal ->
            if (goal.id == goalId) {
                goal.copy(
                    isAchieved = isAchieved,
                    achievedDate = if (isAchieved) LocalDateTime.now() else null
                )
            } else {
                goal
            }
        }
        
        _allGoals.value = updatedGoals
        saveGoalsToPreferences(updatedGoals)
        
        Log.d("GoalsViewModel", "Toggled achievement status for goal ID: $goalId, now achieved: $isAchieved")
    }
    
    /**
     * Deletes a goal by ID.
     */
    fun deleteGoal(goalId: String) {
        val currentGoals = _allGoals.value
        val updatedGoals = currentGoals.filter { it.id != goalId }
        
        if (currentGoals.size != updatedGoals.size) {
            _allGoals.value = updatedGoals
            saveGoalsToPreferences(updatedGoals)
            Log.d("GoalsViewModel", "Deleted goal with ID: $goalId")
        } else {
            Log.w("GoalsViewModel", "Attempted to delete non-existent goal with ID: $goalId")
        }
    }
    
    /**
     * Sets goals that are targeted for the current practice session.
     */
    fun setSessionTargetedGoals(goalIds: List<String>) {
        val targetedGoals = _allGoals.value.filter { goalIds.contains(it.id) }
        _sessionTargetedGoals.value = targetedGoals
        
        Log.d("GoalsViewModel", "Set ${targetedGoals.size} targeted goals for current session")
    }
    
    /**
     * Clears the session targeted goals.
     */
    fun clearSessionTargetedGoals() {
        _sessionTargetedGoals.value = emptyList()
        Log.d("GoalsViewModel", "Cleared targeted goals for session")
    }
    
    /**
     * Returns the IDs of goals targeted for the current session.
     */
    fun getSessionTargetedGoalIds(): List<String> {
        return _sessionTargetedGoals.value.map { it.id }
    }
    
    /**
     * Marks specified goals as achieved at the end of a session.
     */
    fun markGoalsAchievedInSession(achievedGoalIds: List<String>) {
        val currentGoals = _allGoals.value
        val now = LocalDateTime.now()
        
        val updatedGoals = currentGoals.map { goal ->
            if (achievedGoalIds.contains(goal.id)) {
                goal.copy(isAchieved = true, achievedDate = now)
            } else {
                goal
            }
        }
        
        _allGoals.value = updatedGoals
        saveGoalsToPreferences(updatedGoals)
        
        Log.d("GoalsViewModel", "Marked ${achievedGoalIds.size} goals as achieved in session")
    }
    
    /**
     * Converts a TechnicalGoal to JSON for persistence.
     */
    private fun goalToJson(goal: TechnicalGoal): JSONObject {
        return JSONObject().apply {
            put("id", goal.id)
            put("description", goal.description)
            put("createdDate", goal.createdDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            put("lastModifiedDate", goal.lastModifiedDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            put("isAchieved", goal.isAchieved)
            
            // Handle the nullable achievedDate
            if (goal.achievedDate != null) {
                put("achievedDate", goal.achievedDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            } else {
                put("achievedDate", JSONObject.NULL)
            }
        }
    }
    
    /**
     * Converts JSON to a TechnicalGoal.
     */
    private fun jsonToGoal(json: JSONObject): TechnicalGoal {
        val id = json.getString("id")
        val description = json.getString("description")
        val createdDateString = json.getString("createdDate")
        val lastModifiedDateString = json.getString("lastModifiedDate")
        val isAchieved = json.getBoolean("isAchieved")
        
        val createdDate = LocalDateTime.parse(createdDateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val lastModifiedDate = LocalDateTime.parse(lastModifiedDateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        
        // Handle the nullable achievedDate
        val achievedDate = if (json.isNull("achievedDate")) {
            null
        } else {
            LocalDateTime.parse(json.getString("achievedDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
        
        return TechnicalGoal(
            id = id,
            description = description,
            createdDate = createdDate,
            lastModifiedDate = lastModifiedDate,
            achievedDate = achievedDate,
            isAchieved = isAchieved
        )
    }
    
    /**
     * Saves goals to SharedPreferences.
     */
    private fun saveGoalsToPreferences(goals: List<TechnicalGoal>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonArray = JSONArray()
                goals.forEach { goal ->
                    val jsonObject = goalToJson(goal)
                    jsonArray.put(jsonObject)
                }
                
                sharedPrefs.edit()
                    .putString(PREF_KEY_GOALS, jsonArray.toString())
                    .apply()
                
                Log.d("GoalsViewModel", "Saved ${goals.size} goals to SharedPreferences")
            } catch (e: Exception) {
                Log.e("GoalsViewModel", "Error saving goals to SharedPreferences", e)
            }
        }
    }
    
    /**
     * Loads goals from SharedPreferences.
     */
    private fun loadSavedGoals() {
        viewModelScope.launch(Dispatchers.IO) {
            val goalsJson = sharedPrefs.getString(PREF_KEY_GOALS, null)
            val loadedGoals = mutableListOf<TechnicalGoal>()
            
            if (!goalsJson.isNullOrEmpty()) {
                try {
                    val jsonArray = JSONArray(goalsJson)
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val goal = jsonToGoal(jsonObject)
                        loadedGoals.add(goal)
                    }
                    
                    Log.d("GoalsViewModel", "Loaded ${loadedGoals.size} saved goals")
                    
                    withContext(Dispatchers.Main) {
                        _allGoals.value = loadedGoals
                    }
                } catch (e: Exception) {
                    Log.e("GoalsViewModel", "Error loading saved goals", e)
                }
            } else {
                Log.d("GoalsViewModel", "No saved goals found")
            }
        }
    }
} 