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
    }

    private val TAG = "PracticeTrackingService"
    private val CHANNEL_ID = "PracticeTrackingChannel"
    private val NOTIFICATION_ID = 1
    private val MODEL_NAME = "yamnet.tflite"
    private val CLASSIFICATION_INTERVAL_MS = 500L // How often to classify audio
    private val UI_UPDATE_INTERVAL_MS = 1000L // How often to update UI timer (1 second)
    private val MUSIC_CONFIDENCE_THRESHOLD = 0.7f // Increased threshold from 0.5f to 0.7f
    private val NON_PRACTICE_INTERVAL_MS = 8000L // Wait 8 seconds before considering practice stopped

    private var audioRecord: AudioRecord? = null
    private var soundClassifier: AudioClassifier? = null
    private var tensorAudio: TensorAudio? = null
    private lateinit var classificationExecutor: ScheduledExecutorService
    private lateinit var uiUpdateExecutor: ScheduledExecutorService // Separate executor for UI updates
    private var isProcessing = false
    
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

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        classificationExecutor = Executors.newSingleThreadScheduledExecutor()
        uiUpdateExecutor = Executors.newSingleThreadScheduledExecutor() // Initialize UI timer executor
        
        // Initialize TextToSpeech
        initializeTextToSpeech()
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
        classificationExecutor.shutdownNow()
        uiUpdateExecutor.shutdownNow() // Shutdown UI timer executor
        
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

            // Start the classification loop
            classificationExecutor.scheduleAtFixedRate(
                this::runClassification,
                0, // Initial delay
                CLASSIFICATION_INTERVAL_MS, // Interval
                TimeUnit.MILLISECONDS
            )

            // Start the UI update loop
             uiUpdateExecutor.scheduleAtFixedRate(
                 this::updateUiTimer,
                 0, // Initial delay
                 UI_UPDATE_INTERVAL_MS, // Interval (e.g., every second)
                 TimeUnit.MILLISECONDS
             )

            Log.d(TAG, "Classification and UI update tasks scheduled.")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing audio processing", e)
            stopAudioProcessing()
            stopSelf() // Stop the service on fatal error
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
            Log.e(TAG, "Error during classification loop", e)
        }
    }

    // Extracted timer state logic
    // Made internal for testing access, or could be private if tests are in the same module/package
    internal fun updateTimerState(detectedMusic: Boolean, categoryLabel: String, score: Float) {
         // Use injected clock
         val now = clock.getCurrentTimeMillis()
         
         if (detectedMusic) {
             // Any music detection resets the pending stop flag
             if (isPendingMusicStop) {
                 Log.i(TAG, "Music detected during grace period - resetting grace period, continuing practice")
             }
             isPendingMusicStop = false
             lastMusicDetectionTimeMillis = now
             
             if (!isMusicCurrentlyPlaying) {
                 // Music Started (either first time or after a pause)
                 isMusicCurrentlyPlaying = true
                 musicStartTimeMillis = now // Use 'now' from injected clock
                 val status = "Practicing"
                 Log.i(TAG, "Music detected: $categoryLabel (Score: $score) - Starting timer")
                 DetectionStateHolder.updateState(newStatus = status)
             } else {
                 // Music continues - update status if needed
                 val status = "Practicing"
                 Log.d(TAG, "Music continues: $categoryLabel (Score: $score) - Timer keeps running")
                 DetectionStateHolder.updateState(newStatus = status)
             }
         } else {
             // No music detected
             if (isMusicCurrentlyPlaying) {
                 // Check if we're in the grace period
                 if (!isPendingMusicStop) {
                     // First silence detection after music - start grace period
                     isPendingMusicStop = true
                     lastMusicDetectionTimeMillis = now
                     Log.i(TAG, "SILENCE DETECTED: Starting 8-second grace period (${NON_PRACTICE_INTERVAL_MS}ms) - KEEPING TIMER RUNNING")
                     
                     // We still consider practice to be happening during grace period
                     // So we keep the status as "Practicing"
                     DetectionStateHolder.updateState(newStatus = "Practicing")
                 } else {
                     // We're already in grace period, check if 8 seconds passed
                     val silenceDuration = now - lastMusicDetectionTimeMillis
                     if (silenceDuration >= NON_PRACTICE_INTERVAL_MS) {
                         // Grace period over, stop the timer
                         val elapsedMillis = now - musicStartTimeMillis
                         if (elapsedMillis > 0) {
                             accumulatedTimeMillis += elapsedMillis
                         }
                         isMusicCurrentlyPlaying = false
                         musicStartTimeMillis = 0L
                         val status = "" // Empty string when not practicing
                         Log.i(TAG, "GRACE PERIOD EXPIRED: No music for ${silenceDuration}ms (${NON_PRACTICE_INTERVAL_MS}ms threshold). " +
                                 "Practice stopped. Added ${elapsedMillis}ms. Total: ${accumulatedTimeMillis}ms")
                         DetectionStateHolder.updateState(newStatus = status, newTimeMillis = accumulatedTimeMillis)
                     } else {
                         // Still in grace period, CONTINUE counting as practice
                         Log.i(TAG, "WITHIN GRACE PERIOD: Silence for ${silenceDuration}ms out of ${NON_PRACTICE_INTERVAL_MS}ms - KEEPING TIMER RUNNING")
                         
                         // Critical: Keep showing practicing status during grace period
                         DetectionStateHolder.updateState(newStatus = "Practicing")
                     }
                 }
             } else {
                 // Already not playing, just update state
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
             }
         }
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

        // Stop timers first
        // Use try-catch for shutdownNow in case they are already terminated
        try { classificationExecutor.shutdownNow() } catch (e: Exception) { Log.w(TAG, "Error shutting down classificationExecutor", e) }
        try { uiUpdateExecutor.shutdownNow() } catch (e: Exception) { Log.w(TAG, "Error shutting down uiUpdateExecutor", e) }

        // This now uses the injected clock via calculateFinalTime()
        val finalAccumulatedTime = calculateFinalTime()
        
        // Calculate final total session time
        val finalSessionTime = clock.getCurrentTimeMillis() - DetectionStateHolder.state.value.sessionStartTimeMillis

        // Release audio resources
        releaseAudioResources()

        // Update state holder AFTER calculating final time and releasing resources
        DetectionStateHolder.updateState(
            newStatus = "", 
            newTimeMillis = finalAccumulatedTime,
            newTotalSessionTimeMillis = finalSessionTime
        )

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
         } catch (e: Exception) { Log.e(TAG, "Error stopping AudioRecord", e) }
         try {
             audioRecord?.release()
         } catch (e: Exception) { Log.e(TAG, "Error releasing AudioRecord", e) }
         try {
             soundClassifier?.close()
         } catch (e: Exception) { Log.e(TAG, "Error closing SoundClassifier", e) }
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

    private fun createNotification(): Notification {
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
            .setContentText("Tap to return to your practice session")
            .setSmallIcon(android.R.drawable.ic_media_play) // Use a built-in icon as placeholder
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Make it more noticeable
            .setOngoing(true) // Make it non-dismissable
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
} 