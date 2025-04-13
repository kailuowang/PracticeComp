package com.kailuowang.practicecomp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages app settings related to practice detection
 */
class SettingsManager(context: Context) {
    private val TAG = "SettingsManager"
    
    // Default values (same as the original hardcoded constants)
    companion object {
        const val DEFAULT_GRACE_PERIOD_MS = 8 * 1000L // 8 seconds
        const val DEFAULT_AUTO_END_THRESHOLD_MS = 20 * 60 * 1000L // 20 minutes
        
        // SharedPreferences keys
        private const val PREFS_NAME = "PracticeCompSettings"
        private const val KEY_GRACE_PERIOD_MS = "grace_period_ms"
        private const val KEY_AUTO_END_THRESHOLD_MS = "auto_end_threshold_ms"
    }
    
    // SharedPreferences instance
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Returns the configured grace period in milliseconds
     * This is how long the app waits before stopping the timer after music detection stops
     */
    fun getGracePeriodMs(): Long {
        val saved = prefs.getLong(KEY_GRACE_PERIOD_MS, DEFAULT_GRACE_PERIOD_MS)
        Log.d(TAG, "Retrieved grace period: $saved ms")
        return saved
    }
    
    /**
     * Sets the grace period in milliseconds
     */
    fun setGracePeriodMs(milliseconds: Long) {
        if (milliseconds <= 0) {
            Log.w(TAG, "Attempted to set invalid grace period: $milliseconds ms, ignoring")
            return
        }
        
        Log.d(TAG, "Setting grace period to: $milliseconds ms")
        prefs.edit().putLong(KEY_GRACE_PERIOD_MS, milliseconds).apply()
    }
    
    /**
     * Returns the configured auto-end threshold in milliseconds
     * This is how long of silence before the session automatically ends
     */
    fun getAutoEndThresholdMs(): Long {
        val saved = prefs.getLong(KEY_AUTO_END_THRESHOLD_MS, DEFAULT_AUTO_END_THRESHOLD_MS)
        Log.d(TAG, "Retrieved auto-end threshold: $saved ms")
        return saved
    }
    
    /**
     * Sets the auto-end threshold in milliseconds
     */
    fun setAutoEndThresholdMs(milliseconds: Long) {
        if (milliseconds <= 0) {
            Log.w(TAG, "Attempted to set invalid auto-end threshold: $milliseconds ms, ignoring")
            return
        }
        
        Log.d(TAG, "Setting auto-end threshold to: $milliseconds ms")
        prefs.edit().putLong(KEY_AUTO_END_THRESHOLD_MS, milliseconds).apply()
    }
    
    /**
     * Resets all settings to their default values
     */
    fun resetToDefaults() {
        Log.d(TAG, "Resetting all settings to defaults")
        prefs.edit()
            .putLong(KEY_GRACE_PERIOD_MS, DEFAULT_GRACE_PERIOD_MS)
            .putLong(KEY_AUTO_END_THRESHOLD_MS, DEFAULT_AUTO_END_THRESHOLD_MS)
            .apply()
    }
} 