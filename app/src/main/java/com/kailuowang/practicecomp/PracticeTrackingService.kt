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
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
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

    private val TAG = "PracticeTrackingService"
    private val CHANNEL_ID = "PracticeTrackingChannel"
    private val NOTIFICATION_ID = 1
    private val MODEL_NAME = "yamnet.tflite"
    private val CLASSIFICATION_INTERVAL_MS = 500L // How often to classify audio
    private val UI_UPDATE_INTERVAL_MS = 1000L // How often to update UI timer (1 second)
    private val MUSIC_CONFIDENCE_THRESHOLD = 0.5f // Lowered threshold for better detection
    private val NO_MUSIC_THRESHOLD_MS = 8000L // Increased to 8 seconds to handle longer pauses

    private var audioRecord: AudioRecord? = null
    private var soundClassifier: AudioClassifier? = null
    private var tensorAudio: TensorAudio? = null
    private lateinit var classificationExecutor: ScheduledExecutorService
    private lateinit var uiUpdateExecutor: ScheduledExecutorService // Separate executor for UI updates
    private var isProcessing = false

    // Timer State Variables
    @Volatile // Ensure visibility across threads
    private var isMusicCurrentlyPlaying = false
    private var musicStartTimeMillis: Long = 0L
    @Volatile // Ensure visibility across threads
    private var accumulatedTimeMillis: Long = 0L
    @Volatile // Ensure visibility across threads
    private var lastMusicDetectionTimeMillis: Long = 0L // Track when music was last detected

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        classificationExecutor = Executors.newSingleThreadScheduledExecutor()
        uiUpdateExecutor = Executors.newSingleThreadScheduledExecutor() // Initialize UI timer executor
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
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun resetTimerState() {
        isMusicCurrentlyPlaying = false
        musicStartTimeMillis = 0L
        accumulatedTimeMillis = 0L
        lastMusicDetectionTimeMillis = 0L
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
                
                // Log top 3 categories for debugging
                val top3 = categories.sortedByDescending { it.score }.take(3)
                Log.d(TAG, "Top 3 detected categories: " + 
                    top3.joinToString { "${it.label} (${it.score})" })
                
                val musicCategory = categories.filter { category ->
                    category.score > MUSIC_CONFIDENCE_THRESHOLD &&
                    (category.label.contains("music", ignoreCase = true) ||
                     category.label.contains("instrument", ignoreCase = true) ||
                     category.label.contains("singing", ignoreCase = true) ||
                     category.label.contains("guitar", ignoreCase = true) ||
                     category.label.contains("piano", ignoreCase = true) ||
                     category.label.contains("violin", ignoreCase = true) ||
                     category.label.contains("drum", ignoreCase = true) ||
                     category.label.contains("flute", ignoreCase = true) ||
                     category.label.contains("saxophone", ignoreCase = true) ||
                     category.label.contains("trumpet", ignoreCase = true) ||
                     category.label.contains("cello", ignoreCase = true) ||
                     category.label.contains("clarinet", ignoreCase = true) ||
                     category.label.contains("harp", ignoreCase = true) ||
                     category.label.contains("orchestra", ignoreCase = true) ||
                     category.label.contains("musical", ignoreCase = true)
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
                        Log.d(TAG, "Not music: top category $topCategoryLabel with score $topScore")
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
             // Update last detection time whenever music is detected
             lastMusicDetectionTimeMillis = now
             
             if (!isMusicCurrentlyPlaying) {
                 // Music Started
                 isMusicCurrentlyPlaying = true
                 musicStartTimeMillis = now // Use 'now' from injected clock
                 val status = "Practicing"
                 Log.i(TAG, "Music detected: $categoryLabel (Score: $score)")
                 DetectionStateHolder.updateState(newStatus = status)
                 // Time update handled by uiUpdateExecutor
             } else {
                 // Music continues - update status if needed, time handled by UI timer
                 val status = "Practicing"
                 // Log.d(TAG, "Music continues...")
                 DetectionStateHolder.updateState(newStatus = status)
             }
         } else { // !detectedMusic
             if (isMusicCurrentlyPlaying) {
                 // If this is the first time we're detecting no music, set the lastMusicDetectionTimeMillis
                 if (lastMusicDetectionTimeMillis == 0L) {
                     lastMusicDetectionTimeMillis = now;
                 }
                 
                 // Check if we've exceeded the no-music threshold
                 val timeSinceLastMusic = now - lastMusicDetectionTimeMillis
                 
                 if (timeSinceLastMusic >= NO_MUSIC_THRESHOLD_MS) {
                     // Music Stopped (after threshold period)
                     val elapsedMillis = lastMusicDetectionTimeMillis - musicStartTimeMillis // Use last music time instead of now
                     if (elapsedMillis > 0) { // Avoid adding zero or negative time if events are rapid
                          accumulatedTimeMillis += elapsedMillis
                     }
                     isMusicCurrentlyPlaying = false
                     musicStartTimeMillis = 0L
                     val status = "" // Empty string when not practicing
                     Log.i(TAG, "Practice stopped after ${timeSinceLastMusic}ms of silence. Added ${elapsedMillis}ms. Total: ${accumulatedTimeMillis}ms")
                     DetectionStateHolder.updateState(newStatus = status, newTimeMillis = accumulatedTimeMillis)
                 } else {
                     // We're still within the threshold, consider music as still playing
                     Log.d(TAG, "Brief pause in music (${timeSinceLastMusic}ms of ${NO_MUSIC_THRESHOLD_MS}ms threshold), continuing practice")
                     // Keep status as practicing
                     DetectionStateHolder.updateState(newStatus = "Practicing")
                 }
             } else { // !isMusicCurrentlyPlaying
                 // Music remains stopped - update status
                 val status = "" // Empty string when not practicing
                 // Log.d(TAG, "Listening...")
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
     }

    // --- Expose state for testing (use with caution, only for unit tests) ---
    // These allow tests to check internal state without reflection.
    internal fun getAccumulatedTimeMillisForTest(): Long = accumulatedTimeMillis
    internal fun isMusicPlayingForTest(): Boolean = isMusicCurrentlyPlaying
    internal fun getMusicStartTimeMillisForTest(): Long = musicStartTimeMillis
    internal fun getLastMusicDetectionTimeMillisForTest(): Long = lastMusicDetectionTimeMillis
    
    // Allow test to set state to simulate scenarios
    internal fun setTimerStateForTest(isPlaying: Boolean, startTime: Long, accumulatedTime: Long) {
        isMusicCurrentlyPlaying = isPlaying
        musicStartTimeMillis = startTime
        accumulatedTimeMillis = accumulatedTime
        // If we're playing music, initialize lastMusicDetectionTimeMillis to the current time
        if (isPlaying) {
            lastMusicDetectionTimeMillis = clock.getCurrentTimeMillis()
        } else {
            lastMusicDetectionTimeMillis = 0L
        }
    }
    
    // Allow test to set lastMusicDetectionTimeMillis directly
    internal fun setLastMusicDetectionTimeMillisForTest(time: Long) {
        lastMusicDetectionTimeMillis = time
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
        // Intent to open the MainActivity when notification is tapped
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT // Ensure PendingIntent updates if MainActivity state changes
        )

        // TODO: Update content text dynamically based on DetectionStateHolder.state
        // TODO: Add actions to stop the service from the notification

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Practice Companion")
            .setContentText("Monitoring practice session...") // Generic text for now
            // .setSmallIcon(R.drawable.ic_notification_icon) // Replace with your actual icon
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Make it non-dismissable
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
} 