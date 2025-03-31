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
import android.media.MediaRecorder
import android.media.AudioFormat
import android.os.Build
import android.os.IBinder
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

class PracticeTrackingService : Service() {

    private val TAG = "PracticeTrackingService"
    private val CHANNEL_ID = "PracticeTrackingChannel"
    private val NOTIFICATION_ID = 1
    private val MODEL_NAME = "yamnet.tflite" // You'll need to add this model to assets folder

    private var audioRecord: AudioRecord? = null
    private var soundClassifier: AudioClassifier? = null
    private var tensorAudio: TensorAudio? = null
    private lateinit var classificationExecutor: ScheduledExecutorService
    private var isProcessing = false

    // TODO: Add state management for tracking actual practice time

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        classificationExecutor = Executors.newSingleThreadScheduledExecutor()
    }

    @SuppressLint("MissingPermission") // Permissions checked before starting service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (!isProcessing) {
            DetectionStateHolder.updateStatus("Starting...") // Revert status message
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
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    @SuppressLint("MissingPermission")
    private fun startAudioProcessing() {
        Log.d(TAG, "Attempting to start audio processing...")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted. Cannot start processing.")
            return
        }

        try {
            // Initialize TensorFlow Lite AudioClassifier
            val options = AudioClassifierOptions.builder()
                .setBaseOptions(BaseOptions.builder().build())
                .setMaxResults(5)
                .build()
                
            try {
                // Create the classifier
                soundClassifier = AudioClassifier.createFromFileAndOptions(
                    this,
                    MODEL_NAME,
                    options
                )
                
                // Create the audio tensor and recorder
                tensorAudio = soundClassifier?.createInputTensorAudio()
                audioRecord = soundClassifier?.createAudioRecord()
                
                // Start recording
                isProcessing = true
                audioRecord?.startRecording()
                Log.d(TAG, "AudioRecord started recording.")
                
                // Schedule the classification task
                classificationExecutor.scheduleAtFixedRate({
                    if (!isProcessing || audioRecord == null || soundClassifier == null) return@scheduleAtFixedRate
                    
                    try {
                        val classifier = soundClassifier ?: return@scheduleAtFixedRate
                        val audio = tensorAudio ?: return@scheduleAtFixedRate
                        
                        // Load latest audio into tensor
                        audio.load(audioRecord)
                        
                        // Run classification on the audio data
                        val results = classifier.classify(audio)
                        
                        // Process classification results
                        if (results.isNotEmpty() && results[0].categories.isNotEmpty()) {
                            // Check for music-related classifications
                            val musicCategory = results[0].categories.find { 
                                it.label.contains("music", ignoreCase = true) || 
                                it.label.contains("instrument", ignoreCase = true) ||
                                it.label.contains("singing", ignoreCase = true)
                            }
                            
                            if (musicCategory != null && musicCategory.score > 0.6f) {
                                Log.i(TAG, "Music detected! ${musicCategory.label}: ${musicCategory.score}")
                                DetectionStateHolder.updateStatus("Music Detected: ${musicCategory.label}")
                            } else {
                                val topCategory = results[0].categories.maxByOrNull { it.score }
                                val statusMessage = if (topCategory != null) {
                                    "Listening... (Top: ${topCategory.label})"
                                } else {
                                    "Listening... (Low confidence sounds)"
                                }
                                DetectionStateHolder.updateStatus(statusMessage)
                                
                                if (topCategory != null) {
                                    Log.d(TAG, "No strong Music detection. Top sound: ${topCategory.label} (${topCategory.score})")
                                } else {
                                    Log.d(TAG, "No strong Music detection. Low confidence sounds.")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during classification loop", e)
                    }
                }, 0, 500, TimeUnit.MILLISECONDS)
                Log.d(TAG, "Classification task scheduled.")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create AudioClassifier", e)
                return
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing audio processing", e)
            stopAudioProcessing()
        }
    }

    private fun stopAudioProcessing() {
        if (!isProcessing) return
        Log.d(TAG, "Stopping audio processing...")
        isProcessing = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            soundClassifier?.close()
            soundClassifier = null
            tensorAudio = null
            Log.d(TAG, "AudioRecord stopped and released.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        DetectionStateHolder.updateStatus("Idle")
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
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // TODO: Add actions to stop the service from the notification

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Practice Companion")
            .setContentText("Actively listening for practice...") // TODO: Update text based on state
            // .setSmallIcon(R.drawable.ic_notification_icon) // Replace with your actual icon
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Make it non-dismissable
            .build()
    }

    // Binding is not used in this case
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
} 