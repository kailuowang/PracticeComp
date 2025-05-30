package com.kailuowang.practicecomp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.audio.classifier.AudioClassifier.AudioClassifierOptions

class PracticeTrackingService(
    // Inject Clock dependency, default to SystemClock for production
    private val clock: Clock = SystemClock
) : Service() {

    companion object {
        // Static variable to track if service is running
        @Volatile
        var isServiceRunning = false
            private set
            
        // Utility function to check if app is in debug mode
        fun isDebugMode(): Boolean = BuildConfig.DEBUG
    }

    private val TAG = "PracticeTrackingService"
    private val CHANNEL_ID = "PracticeTrackingChannel"
    private val NOTIFICATION_ID = 1
    private val MODEL_NAME = "yamnet.tflite" // Keep this as a member variable
    private val CLASSIFICATION_INTERVAL_MS = 1000L // How often to classify audio (changed to 1 second)
    private val UI_UPDATE_INTERVAL_MS = 1000L // How often to update UI timer (1 second)
    private val MUSIC_CONFIDENCE_THRESHOLD = 0.7f // Increased threshold from 0.5f to 0.7f
    private val HEALTH_CHECK_INTERVAL_MS = 30000L // Health check every 30 seconds
    
    // Settings - now initialized at runtime from SettingsManager
    private lateinit var settingsManager: SettingsManager
    private var gracePeriodMs: Long = SettingsManager.DEFAULT_GRACE_PERIOD_MS
    private var autoEndThresholdMs: Long = SettingsManager.DEFAULT_AUTO_END_THRESHOLD_MS
    
    private var audioRecord: AudioRecord? = null
    private var soundClassifier: AudioClassifier? = null
    private var tensorAudio: TensorAudio? = null
    private lateinit var classificationExecutor: ScheduledExecutorService
    private lateinit var uiUpdateExecutor: ScheduledExecutorService // Separate executor for UI updates
    private lateinit var healthCheckExecutor: ScheduledExecutorService // Executor for health checks
    private var isProcessing = false
    
    // Tracking variables for health monitoring
    private var lastClassificationTimeMs = 0L
    private var lastUiUpdateTimeMs = 0L
    private var classificationCount = 0
    private var uiUpdateCount = 0
    
    // TextToSpeech
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var goalReachedAnnounced = false

    // Timer State Variables
    @Volatile // Ensure visibility across threads
    private var isMusicCurrentlyPlaying = false
    private var musicStartTimeMillis: Long = 0L
    @Volatile // Ensure visibility across threads
    private var accumulatedTimeMillis: Long = 0L
    private var lastMusicDetectionTimeMillis: Long = 0L // Track when music was last detected
    private var isPendingMusicStop: Boolean = false // Flag to track if we're in the "grace period"

    // Test-only flag to verify auto-end was triggered
    private var _wasAutoEndTriggered = false
    internal fun wasAutoEndTriggeredForTest(): Boolean = _wasAutoEndTriggered
    internal fun resetAutoEndTriggerFlagForTest() { _wasAutoEndTriggered = false }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        classificationExecutor = Executors.newSingleThreadScheduledExecutor()
        uiUpdateExecutor = Executors.newSingleThreadScheduledExecutor() // Initialize UI timer executor
        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor() // Initialize health check executor
        
        // Initialize settings manager and load settings
        settingsManager = SettingsManager(this)
        loadSettings()
        
        // Initialize TextToSpeech
        initializeTextToSpeech()
    }
    
    // Load settings from the SettingsManager
    private fun loadSettings() {
        gracePeriodMs = settingsManager.getGracePeriodMs()
        autoEndThresholdMs = settingsManager.getAutoEndThresholdMs()
        
        Log.d(TAG, "Loaded settings - Grace period: $gracePeriodMs ms, Auto-end threshold: $autoEndThresholdMs ms")
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "This Language is not supported for TTS")
                } else {
                    isTtsInitialized = true
                    textToSpeech?.setSpeechRate(0.8f) // Slightly slower rate for clarity
                    Log.d(TAG, "TextToSpeech initialized successfully")
                }
            } else {
                Log.e(TAG, "Initialization failed for TextToSpeech")
            }
        }
    }
    
    private fun speakText(text: String) {
        if (isTtsInitialized && textToSpeech != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "goalReached")
            } else {
                @Suppress("DEPRECATION")
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            }
            Log.d(TAG, "Speaking: $text")
        } else {
            Log.e(TAG, "TextToSpeech not initialized, cannot speak")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Check and log battery optimization status
        checkBatteryOptimizationStatus()

        if (!isProcessing) {
            resetTimerState() // Reset timer when starting
            DetectionStateHolder.resetState() // Reset UI state
            startAudioProcessing()
        }
        
        // Reset goal reached flag when starting service
        goalReachedAnnounced = false
        
        // Set static flag that service is running
        isServiceRunning = true

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        if (isProcessing) {
            stopAudioProcessing()
        }
        
        // Ensure executors are shut down
        if (!classificationExecutor.isShutdown) classificationExecutor.shutdownNow()
        if (!uiUpdateExecutor.isShutdown) uiUpdateExecutor.shutdownNow()
        if (!healthCheckExecutor.isShutdown) healthCheckExecutor.shutdownNow()
        
        // Shutdown TextToSpeech
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isTtsInitialized = false
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        
        // Clear static flag when service is destroyed
        isServiceRunning = false
    }

    private fun resetTimerState() {
        isMusicCurrentlyPlaying = false
        musicStartTimeMillis = 0L
        accumulatedTimeMillis = 0L
        lastMusicDetectionTimeMillis = 0L
        isPendingMusicStop = false
        goalReachedAnnounced = false
        lastAnnouncedMilestone = 0 // Reset milestone tracking
    }

    @SuppressLint("MissingPermission")
    private fun startAudioProcessing() {
        Log.d(TAG, "Attempting to start audio processing...")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted. Cannot start processing.")
            stopSelf() // Stop service if permissions are missing
            return
        }

        try {
            val options = AudioClassifierOptions.builder()
                .setBaseOptions(BaseOptions.builder().build())
                .setMaxResults(5) // Get a few top results
                .build()

            soundClassifier = AudioClassifier.createFromFileAndOptions(this, MODEL_NAME, options)
            tensorAudio = soundClassifier?.createInputTensorAudio()
            audioRecord = soundClassifier?.createAudioRecord()

            if (audioRecord == null || soundClassifier == null || tensorAudio == null) {
                 Log.e(TAG, "Failed to initialize TFLite Audio components.")
                 stopAudioProcessing()
                 return
            }

            isProcessing = true
            audioRecord?.startRecording()
            Log.d(TAG, "AudioRecord started recording.")
            
            // Reset health monitoring counters
            lastClassificationTimeMs = System.currentTimeMillis()
            lastUiUpdateTimeMs = System.currentTimeMillis()
            classificationCount = 0
            uiUpdateCount = 0

            // Start the classification loop
            classificationExecutor.scheduleAtFixedRate(
                this::runClassificationWithErrorHandling, // Use wrapper method with error handling
                0, // Initial delay
                CLASSIFICATION_INTERVAL_MS, // Interval
                TimeUnit.MILLISECONDS
            )

            // Start the UI update loop
             uiUpdateExecutor.scheduleAtFixedRate(
                 this::updateUiTimerWithErrorHandling, // Use wrapper method with error handling
                 0, // Initial delay
                 UI_UPDATE_INTERVAL_MS, // Interval (e.g., every second)
                 TimeUnit.MILLISECONDS
             )
             
            // Start health check timer
            healthCheckExecutor.scheduleAtFixedRate(
                this::performHealthCheck,
                HEALTH_CHECK_INTERVAL_MS, // Initial delay
                HEALTH_CHECK_INTERVAL_MS, // Interval
                TimeUnit.MILLISECONDS
            )

            Log.d(TAG, "Classification, UI update, and health check tasks scheduled.")

        } catch (e: Exception) {
            handleException(e, "Error initializing audio processing")
            stopAudioProcessing()
            stopSelf() // Stop the service on fatal error
        }
    }
    
    // Wrapper method with error handling for classification
    private fun runClassificationWithErrorHandling() {
        try {
            // Update health monitoring data
            lastClassificationTimeMs = System.currentTimeMillis()
            classificationCount++
            
            runClassification()
        } catch (e: Exception) {
            handleException(e, "Exception in classification loop")
            // In debug mode, we might want to rethrow to crash the app and get developer's attention
            // In production, we'll try to recover
            if (!isDebugMode()) {
                // Try to recover by resetting audio processing
                try {
                    Log.w(TAG, "Attempting to recover from error by restarting audio processing")
                    stopAudioProcessing()
                    startAudioProcessing()
                } catch (recoveryException: Exception) {
                    Log.e(TAG, "Failed to recover from error", recoveryException)
                    stopSelf() // Stop service if recovery fails
                }
            } else {
                // In debug mode, we'll stop processing to make the error more obvious
                stopAudioProcessing()
            }
        }
    }
    
    // Wrapper method with error handling for UI updates
    private fun updateUiTimerWithErrorHandling() {
        try {
            // Update health monitoring data
            lastUiUpdateTimeMs = System.currentTimeMillis()
            uiUpdateCount++
            
            updateUiTimer()
        } catch (e: Exception) {
            handleException(e, "Exception in UI timer update")
        }
    }

    private fun runClassification() {
        if (!isProcessing || audioRecord == null || soundClassifier == null || tensorAudio == null) {
            return
        }

        try {
            val classifier = soundClassifier!!
            val audio = tensorAudio!!
            val record = audioRecord!!

            audio.load(record)
            val results = classifier.classify(audio)

            var detectedMusic = false
            var topCategoryLabel = "Silence/Unknown"
            var topScore = 0f

            if (results.isNotEmpty() && results[0].categories.isNotEmpty()) {
                val categories = results[0].categories
                
                // More specific filter for musical instruments
                val musicCategory = categories.filter { category ->
                    category.score > MUSIC_CONFIDENCE_THRESHOLD && (
                        // Specific instrument patterns
                        category.label.contains("musical instrument", ignoreCase = true) ||
                        category.label.contains("guitar", ignoreCase = true) ||
                        category.label.contains("piano", ignoreCase = true) ||
                        category.label.contains("violin", ignoreCase = true) ||
                        category.label.contains("trumpet", ignoreCase = true) ||
                        category.label.contains("saxophone", ignoreCase = true) ||
                        category.label.contains("flute", ignoreCase = true) ||
                        category.label.contains("drum", ignoreCase = true) ||
                        category.label.contains("bass", ignoreCase = true) ||
                        // Generic music patterns
                        (category.label.contains("music", ignoreCase = true) && 
                         !category.label.contains("background", ignoreCase = true)) ||
                        // Singing only with high confidence
                        (category.label.contains("singing", ignoreCase = true) && 
                         category.score > 0.8f)
                        // Speech is removed from detection
                    )
                }.maxByOrNull { it.score }

                if (musicCategory != null) {
                    detectedMusic = true
                    topCategoryLabel = musicCategory.label
                    topScore = musicCategory.score
                    Log.d(TAG, "Detected music: $topCategoryLabel with score $topScore")
                } else {
                    val topOverall = categories.maxByOrNull { it.score }
                    if (topOverall != null) {
                        topCategoryLabel = topOverall.label
                        topScore = topOverall.score
                        Log.d(TAG, "Not music: $topCategoryLabel with score $topScore")
                    }
                }
            }

            // Call the separated timer logic function
            updateTimerState(detectedMusic, topCategoryLabel, topScore)

        } catch (e: Exception) {
            handleException(e, "Error during classification loop")
            throw e // Rethrow to be caught by the wrapper method
        }
    }

    // Extracted timer state logic
    // Made internal for testing access, or could be private if tests are in the same module/package
    internal fun updateTimerState(detectedMusic: Boolean, categoryLabel: String, score: Float) {
         // Use injected clock
         val now = clock.getCurrentTimeMillis()
         
         if (detectedMusic) {
           // Music Detected Logic (Reset grace period, start/continue timer)
           if (isPendingMusicStop) {
               Log.i(TAG, "Music detected during grace period - resetting grace period, continuing practice")
           }
           isPendingMusicStop = false

           if (!isMusicCurrentlyPlaying) {
               // Music Started
               isMusicCurrentlyPlaying = true
               musicStartTimeMillis = now
               Log.i(TAG, "Music detected: $categoryLabel (Score: $score) - Starting timer")
               DetectionStateHolder.updateState(newStatus = "Practicing")
           } else {
               // Music continues
               Log.d(TAG, "Music continues: $categoryLabel (Score: $score) - Timer keeps running")
               DetectionStateHolder.updateState(newStatus = "Practicing") // Ensure status stays Practicing
           }
         } else {
             // No music detected

             // --- Check for Auto-End FIRST --- 
             // Check if enough time has passed since the *last* music was heard
             if (lastMusicDetectionTimeMillis > 0 && (now - lastMusicDetectionTimeMillis >= autoEndThresholdMs)) {
                 Log.i(TAG, "TEST_LOG: Auto-end condition MET in updateTimerState. now=$now, lastMusic=$lastMusicDetectionTimeMillis, threshold=$autoEndThresholdMs")
                 Log.i(TAG, "AUTO-END TRIGGERED: Total silence duration (${now - lastMusicDetectionTimeMillis} ms) exceeded threshold ($autoEndThresholdMs ms).")
                 // The actual end time is when the grace period *would have finished* if it hadn't been interrupted by auto-end
                 // Or simply, the time music last stopped + grace period duration
                 val actualEndTime = lastMusicDetectionTimeMillis + gracePeriodMs 
                 autoEndSession(actualEndTime)
                 return // Stop further processing in this cycle
             }
             // --- End Check for Auto-End ---

             if (isMusicCurrentlyPlaying) {
                 // Check if we're in the grace period
                 if (!isPendingMusicStop) {
                     // First silence detection after music - start grace period
                     isPendingMusicStop = true
                     lastMusicDetectionTimeMillis = now // Set the time when silence *started*
                     Log.i(TAG, "SILENCE DETECTED: Starting grace period (${gracePeriodMs}ms) - KEEPING TIMER RUNNING")
                     
                     // We still consider practice to be happening during grace period
                     // So we keep the status as "Practicing"
                     DetectionStateHolder.updateState(newStatus = "Practicing")
                 } else {
                     // We're already in grace period, check if grace period time passed
                     val silenceDuration = now - lastMusicDetectionTimeMillis
                     if (silenceDuration >= gracePeriodMs) {
                         // Grace period over
                         val practiceSegmentDuration = (lastMusicDetectionTimeMillis + gracePeriodMs) - musicStartTimeMillis
                         if (practiceSegmentDuration > 0) {
                             accumulatedTimeMillis += practiceSegmentDuration
                             Log.i(TAG, "Grace period ended. Added practice segment of $practiceSegmentDuration ms.")
                         }
                         isMusicCurrentlyPlaying = false
                         musicStartTimeMillis = 0L
                         val status = "" // Empty string when not practicing
                         Log.i(TAG, "GRACE PERIOD EXPIRED: No music for ${silenceDuration}ms (${gracePeriodMs}ms threshold). " +
                                 "Practice stopped. Total accumulated: ${accumulatedTimeMillis}ms")
                         DetectionStateHolder.updateState(newStatus = status, newTimeMillis = accumulatedTimeMillis)
                     } else {
                         // Still in grace period, CONTINUE counting as practice
                         Log.i(TAG, "WITHIN GRACE PERIOD: Silence for ${silenceDuration}ms out of ${gracePeriodMs}ms - KEEPING TIMER RUNNING")
                         
                         // Critical: Keep showing practicing status during grace period
                         DetectionStateHolder.updateState(newStatus = "Practicing")
                     }
                 }
             } else {
                 // Already not playing (and auto-end didn't trigger), just update state
                 val status = "" // Empty string when not practicing
                 Log.d(TAG, "No practice detected - Timer stopped")
                 DetectionStateHolder.updateState(newStatus = status, newTimeMillis = accumulatedTimeMillis)
             }
         }
     }

    // Made internal for testing access
    internal fun updateUiTimer() {
         if (!isProcessing) return // Check if service should be processing (though test might call directly)

         var currentDisplayTime = accumulatedTimeMillis
         if (isMusicCurrentlyPlaying && musicStartTimeMillis > 0) {
             // Use injected clock for current time calculation
             currentDisplayTime += (clock.getCurrentTimeMillis() - musicStartTimeMillis)
         }
         
         // Calculate total session time
         val totalSessionTime = clock.getCurrentTimeMillis() - DetectionStateHolder.state.value.sessionStartTimeMillis
         
         // Log the value just before updating the state holder
         Log.d(TAG, "[updateUiTimer] Calculated display time: $currentDisplayTime, Total session time: $totalSessionTime") 
         
         DetectionStateHolder.updateState(
             newTimeMillis = currentDisplayTime,
             newTotalSessionTimeMillis = totalSessionTime
         )
         
         // Check for goal reached
         checkGoalReached(currentDisplayTime)

         // --- Update Foreground Notification ---
         try {
             val viewModel = PracticeAppContainer.provideViewModel(application)
             val goalMinutes = viewModel.uiState.value.goalMinutes
             val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

             val remainingMillis = calculateRemainingMillis(currentDisplayTime, goalMinutes)
             val notificationText = if (goalMinutes > 0) {
                 "${formatMillisToMmSs(remainingMillis)} remaining"
             } else {
                 "Practiced: ${formatMillisToMmSs(currentDisplayTime)}"
             }

             // Re-use the existing notification creation logic but update the text
             val updatedNotification = createNotification(notificationText)

             notificationManager.notify(NOTIFICATION_ID, updatedNotification)
             Log.d(TAG, "[updateUiTimer] Notification updated: $notificationText")
         } catch (e: Exception) {
             handleException(e, "Error updating notification")
         }
         // --- End Notification Update ---
     }
     
     // Check if practice goal has been reached and announce it
     private fun checkGoalReached(currentDisplayTime: Long) {
         val viewModel = PracticeAppContainer.provideViewModel(application)
         val currentState = viewModel.uiState.value
         
         // Check if goal is set and reached
         if (currentState.goalMinutes > 0 && !goalReachedAnnounced) {
             val goalMillis = currentState.goalMinutes * 60 * 1000L
             
             if (currentDisplayTime >= goalMillis) {
                 Log.d(TAG, "Practice goal reached! ${currentState.goalMinutes} minutes")
                 
                 // Announce goal reached
                 speakText("You did it! Goal reached.")
                 
                 // Set flag to not repeat announcement
                 goalReachedAnnounced = true
             } else {
                 // Time reminder feature: Announce progress at 25%, 50%, 75% intervals
                 checkProgressMilestones(currentDisplayTime, goalMillis, currentState.goalMinutes)
             }
         }
     }
     
     // Track progress milestones at 25% intervals
     private var lastAnnouncedMilestone = 0
     
     private fun checkProgressMilestones(currentDisplayTime: Long, goalMillis: Long, goalMinutes: Int) {
         // Calculate current progress percentage
         val progressPercentage = (currentDisplayTime * 100 / goalMillis).toInt()
         
         // Check if we've hit a new 25% milestone
         val currentMilestone = progressPercentage / 25
         
         // Only announce if this is a new milestone (25%, 50%, or 75%)
         if (currentMilestone > lastAnnouncedMilestone && currentMilestone in 1..3) {
             // Calculate remaining time precisely
             val remainingMillis = calculateRemainingMillis(currentDisplayTime, goalMinutes)
             // Convert to nearest minute for the announcement
             val remainingMinutesForAnnouncement = ((remainingMillis / 1000.0 / 60.0) + 0.5).toInt()
             
             // Log the milestone
             Log.d(TAG, "Practice milestone reached: ${currentMilestone * 25}%, $remainingMinutesForAnnouncement minutes left (calculated)")
             
             // Announce the milestone
             speakText("Good progress! $remainingMinutesForAnnouncement minutes left.")
             
             // Update the last announced milestone
             lastAnnouncedMilestone = currentMilestone
         }
     }

    // Helper function to calculate remaining milliseconds towards a goal
    private fun calculateRemainingMillis(currentPracticeMillis: Long, goalMinutes: Int): Long {
        if (goalMinutes <= 0) {
            return 0L // No goal set or invalid goal
        }
        val goalMillis = goalMinutes * 60 * 1000L
        return maxOf(0L, goalMillis - currentPracticeMillis)
    }

    // --- Expose state for testing (use with caution, only for unit tests) ---
    // These allow tests to check internal state without reflection.
    internal fun getAccumulatedTimeMillisForTest(): Long = accumulatedTimeMillis
    internal fun isMusicPlayingForTest(): Boolean = isMusicCurrentlyPlaying
    internal fun getMusicStartTimeMillisForTest(): Long = musicStartTimeMillis
    // Allow test to set state to simulate scenarios
    internal fun setTimerStateForTest(isPlaying: Boolean, startTime: Long, accumulatedTime: Long) {
        isMusicCurrentlyPlaying = isPlaying
        musicStartTimeMillis = startTime
        accumulatedTimeMillis = accumulatedTime
    }
    // Allow test to manually set processing flag if needed, although direct method calls are often better
    internal fun setProcessingFlagForTest(processing: Boolean) {
        isProcessing = processing
    }

    private fun stopAudioProcessing() {
        if (!isProcessing) return
        Log.d(TAG, "Stopping audio processing...")
        isProcessing = false

        // Log executor states before shutting down
        Log.d(TAG, "Classification executor shutdown state: isShutdown=${classificationExecutor.isShutdown}, isTerminated=${classificationExecutor.isTerminated}")
        Log.d(TAG, "UI update executor shutdown state: isShutdown=${uiUpdateExecutor.isShutdown}, isTerminated=${uiUpdateExecutor.isTerminated}")
        Log.d(TAG, "Health check executor shutdown state: isShutdown=${healthCheckExecutor.isShutdown}, isTerminated=${healthCheckExecutor.isTerminated}")

        // Stop executors using the helper method
        shutdownExecutors()

        // This now uses the injected clock via calculateFinalTime()
        val finalAccumulatedTime = calculateFinalTime()
        
        // Calculate final total session time
        val finalSessionTime = clock.getCurrentTimeMillis() - DetectionStateHolder.state.value.sessionStartTimeMillis

        // Release audio resources
        releaseAudioResources()

        // Save session using the centralized method before resetting state
        saveSessionIfMeaningful(finalSessionTime, finalAccumulatedTime)

        resetTimerState() // Reset internal timer state variables
        Log.d(TAG, "Audio processing stopped and resources released. Final accumulated time: $finalAccumulatedTime ms, Total session time: $finalSessionTime ms")
    }

    // Helper to calculate final time segment before resetting state
    private fun calculateFinalTime(): Long {
        var finalTime = accumulatedTimeMillis
        if (isMusicCurrentlyPlaying && musicStartTimeMillis > 0) {
            // Use injected clock
            val elapsedMillis = clock.getCurrentTimeMillis() - musicStartTimeMillis
            if (elapsedMillis > 0) {
                 finalTime += elapsedMillis
                 Log.i(TAG, "Service stopping mid-practice. Added final ${elapsedMillis}ms. Total: ${finalTime}ms")
            }
        }
        return finalTime
    }

    // Helper to release audio resources safely
    private fun releaseAudioResources() {
         try {
             audioRecord?.stop()
         } catch (e: Exception) { 
             handleException(e, "Error stopping AudioRecord")
         }
         try {
             audioRecord?.release()
         } catch (e: Exception) { 
             handleException(e, "Error releasing AudioRecord")
         }
         try {
             soundClassifier?.close()
         } catch (e: Exception) { 
             handleException(e, "Error closing SoundClassifier")
         }
         audioRecord = null
         soundClassifier = null
         tensorAudio = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Practice Tracking Service Channel",
                NotificationManager.IMPORTANCE_LOW // Low importance to minimize interruption
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    // Overload createNotification to accept dynamic text
    private fun createNotification(contentText: String = "Tap to return to your practice session"): Notification {
        // Intent to open directly to the practice session screen when notification is tapped
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            // Add flags to clear any existing activities and start fresh
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            
            // Add extra to indicate we want to go directly to the session screen
            putExtra("navigate_to", AppDestinations.PRACTICE_SESSION)
        }
        
        // Using different flag combinations based on API level for maximum compatibility
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, // Request code
            notificationIntent, 
            pendingIntentFlags
        )

        Log.d(TAG, "Creating notification with intent extras: ${notificationIntent.extras?.keySet()?.joinToString() ?: "null"}")

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Practice Companion")
            .setContentText(contentText) // Use the provided or default content text
            .setSmallIcon(android.R.drawable.ic_media_play) // Use a built-in icon as placeholder
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Make it more noticeable
            .setOngoing(true) // Make it non-dismissable
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Make visible on lock screen
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Helper method to handle exceptions based on build type
    private fun handleException(e: Exception, message: String) {
        if (isDebugMode()) {
            // In debug mode, log detailed error and notify the user
            Log.e(TAG, "$message: ${e.message}", e)
            
            // Show a notification to the developer about the error
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create a debug channel if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val debugChannel = NotificationChannel(
                    "debug_channel",
                    "Debug Errors",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(debugChannel)
            }
            
            // Create and show the notification
            val errorNotification = NotificationCompat.Builder(this, "debug_channel")
                .setContentTitle("Practice Companion Debug Error")
                .setContentText("$message: ${e.message}")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$message: ${e.message}\n${e.stackTraceToString().take(500)}..."))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
                
            notificationManager.notify(9999, errorNotification)
        } else {
            // In production mode, just log the error
            Log.e(TAG, message, e)
        }
    }

    /**
     * Checks if battery optimization is enabled for the app and logs the status.
     * Battery optimization can cause background services to be killed or restricted.
     */
    private fun checkBatteryOptimizationStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            
            val isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(packageName)
            Log.i(TAG, "Battery optimization status: ${if (isIgnoringBatteryOptimizations) "IGNORED (good)" else "ENABLED (may restrict service)"}")
            
            if (!isIgnoringBatteryOptimizations && isDebugMode()) {
                // In debug mode, log a warning instead of creating an exception
                Log.w(TAG, "Performance Warning: Battery optimization is enabled and may restrict service")
            }
        } else {
            Log.i(TAG, "Device running Android < 6.0, no battery optimization check needed")
        }
    }

    /**
     * Performs a health check on the service to ensure audio processing is still active.
     * If processing appears to have stopped, attempts to restart it.
     */
    private fun performHealthCheck() {
        try {
            val now = System.currentTimeMillis()
            val classificationAge = now - lastClassificationTimeMs
            val uiUpdateAge = now - lastUiUpdateTimeMs
            
            Log.d(TAG, "Health check: isProcessing=$isProcessing, " +
                 "classification [count=$classificationCount, lastRun=${classificationAge}ms ago], " +
                 "UI update [count=$uiUpdateCount, lastRun=${uiUpdateAge}ms ago]")
            
            // Check if processing flag is true but no activity has occurred recently
            if (isProcessing) {
                // Check if classification hasn't run for 3x its expected interval
                if (classificationAge > 3 * CLASSIFICATION_INTERVAL_MS) {
                    Log.w(TAG, "Health check detected stalled classification loop (${classificationAge}ms since last run)")
                    
                    // If in debug mode, show notification
                    if (isDebugMode()) {
                        handleException(
                            Exception("Classification loop stalled for ${classificationAge}ms"),
                            "Service Health Warning"
                        )
                    }
                    
                    // Attempt recovery
                    Log.i(TAG, "Health check attempting recovery by restarting audio processing")
                    stopAudioProcessing()
                    startAudioProcessing()
                    return
                }
                
                // Check if UI updates haven't run for 3x their expected interval
                if (uiUpdateAge > 3 * UI_UPDATE_INTERVAL_MS) {
                    Log.w(TAG, "Health check detected stalled UI update loop (${uiUpdateAge}ms since last run)")
                    
                    // If in debug mode, show notification
                    if (isDebugMode()) {
                        handleException(
                            Exception("UI update loop stalled for ${uiUpdateAge}ms"),
                            "Service Health Warning"
                        )
                    }
                    
                    // Less severe - might recover on its own
                    // We could restart this specific task if needed
                }
            } else {
                // Service says it's not processing - this shouldn't happen unless we're shutting down
                Log.w(TAG, "Health check found isProcessing=false while service is running")
                
                // If the service should be running but isn't processing, restart
                if (isServiceRunning) {
                    Log.i(TAG, "Health check restarting audio processing because isServiceRunning=true but isProcessing=false")
                    startAudioProcessing()
                }
            }
        } catch (e: Exception) {
            // Catch and log, but don't crash - health check should be robust
            Log.e(TAG, "Error in health check", e)
        }
    }

    // Helper function to format milliseconds to MM:SS string
    private fun formatMillisToMmSs(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    // Centralized saving logic
    private fun saveSessionIfMeaningful(totalTimeMillis: Long, practiceTimeMillis: Long) {
        if (totalTimeMillis > 5000) { // Use the same threshold (5 seconds)
            try {
                val viewModel = PracticeAppContainer.provideViewModel(application)
                Log.d(TAG, "Saving session via ViewModel - Total: $totalTimeMillis, Practice: $practiceTimeMillis")
                viewModel.saveSession(
                    totalTimeMillis = totalTimeMillis,
                    practiceTimeMillis = practiceTimeMillis
                )
            } catch (e: Exception) {
                handleException(e, "Error saving session") // Generic error message
            }
        } else {
            Log.d(TAG, "Session too short to save (Total: $totalTimeMillis ms)")
        }
    }

    // Handles the automatic session end logic
    private fun autoEndSession(actualEndTimeMillis: Long) {
        Log.i(TAG, "TEST_LOG: Entering autoEndSession. actualEndTime=$actualEndTimeMillis")
        if (!isProcessing) {
            Log.w(TAG, "TEST_LOG: Exiting autoEndSession early because !isProcessing")
            return // Avoid multiple triggers if already stopping
        }
        _wasAutoEndTriggered = true // Set test flag
        Log.i(TAG, "Executing autoEndSession...")
        isProcessing = false // Prevent further processing loop calls

        // Shutdown executors immediately
        shutdownExecutors()

        // Practice time is already correctly calculated up to the end of the grace period
        val finalPracticeTime = accumulatedTimeMillis

        // Calculate final session time based on when music actually stopped (end of grace period)
        val sessionStartTime = DetectionStateHolder.state.value.sessionStartTimeMillis
        val finalTotalSessionTime = if (sessionStartTime > 0) {
            actualEndTimeMillis - sessionStartTime
        } else {
             Log.w(TAG, "Session start time was not positive during auto-end. Total time set to 0.")
            0L // Fallback, should ideally not happen if service started correctly
        }

        // Log final values
        Log.d(TAG, "Auto-ending session. Actual End Time (end of grace period): $actualEndTimeMillis, Start Time: $sessionStartTime")
        Log.d(TAG, "Final Practice Time: $finalPracticeTime ms, Final Total Session Time: $finalTotalSessionTime ms")

        // Release audio resources
        releaseAudioResources()

        // Save session using the centralized method
        saveSessionIfMeaningful(finalTotalSessionTime, finalPracticeTime)

        // Update state holder to reflect stopped state (optional but good practice)
        // Use specific status to indicate auto-end if needed later
        DetectionStateHolder.updateState(
            newStatus = "Session Auto-Ended", 
            newTimeMillis = finalPracticeTime,
            newTotalSessionTimeMillis = finalTotalSessionTime
        )

        // Reset internal timer state variables (might be redundant with service stopping)
        resetTimerState()

        // Stop the service itself
        Log.i(TAG, "Stopping service due to auto-end.")
        stopSelf() // This will eventually trigger onDestroy
    }

    // Helper to shutdown executors cleanly
    private fun shutdownExecutors() {
        Log.d(TAG, "Shutting down executors...")
        if (!classificationExecutor.isShutdown) {
            try { classificationExecutor.shutdownNow() } catch (e: Exception) { handleException(e, "Error shutting down classificationExecutor") }
        }
        if (!uiUpdateExecutor.isShutdown) {
            try { uiUpdateExecutor.shutdownNow() } catch (e: Exception) { handleException(e, "Error shutting down uiUpdateExecutor") }
        }
        if (!healthCheckExecutor.isShutdown) {
            try { healthCheckExecutor.shutdownNow() } catch (e: Exception) { handleException(e, "Error shutting down healthCheckExecutor") }
        }
        Log.d(TAG, "Executors shut down.")
    }
} 