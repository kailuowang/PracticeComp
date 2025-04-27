# Technical Notes: Music Play Time Detection

This document outlines the technical approach and considerations for implementing the automatic detection of music playing time in a background process.

## High-Level Approach (Using TensorFlow Lite AudioClassifier)

1.  **Background Service:** A foreground service is required to reliably capture and process audio when the app is not in the foreground. This service will display a persistent notification.
2.  **Audio Capture:** Utilize TensorFlow Lite's `AudioRecord` wrapper to access the microphone input stream.
3.  **Audio Classification:**
    *   Use TensorFlow Lite's `AudioClassifier` API with the YAMNet pre-trained model.
    *   Feed audio buffers into the `AudioClassifier` using `TensorAudio`.
    *   The YAMNet model can identify 521 different sound classes, including various music categories, instruments, and singing.
    *   Monitor the classification results for music-related categories.
4.  **Detection Logic:**
    *   Define a confidence threshold for music-related classifications.
    *   Filter classification results for music-related labels (e.g., "music", "instrument", "singing").
    *   Implement logic to determine when music playing starts (e.g., music category consistently above threshold for X seconds) and stops (e.g., drops below threshold for Y seconds).
5.  **Logging:** When music playing is detected, start/stop timers to log the duration accurately.

## Key APIs and Libraries 

*   **Core:**
    *   `android.app.Service` / `android.app.ForegroundService`
    *   `org.tensorflow.lite.task.audio.classifier.AudioClassifier`
    *   `org.tensorflow.lite.support.audio.TensorAudio`
    *   `android.Manifest.permission.RECORD_AUDIO`
    *   `android.Manifest.permission.FOREGROUND_SERVICE`
    *   `android.Manifest.permission.POST_NOTIFICATIONS` (Android 13+)
*   **Dependencies:**
    *   `org.tensorflow:tensorflow-lite-task-audio:0.4.4`
    *   `org.tensorflow:tensorflow-lite-support:0.4.4`

## Challenges and Considerations

*   **Accuracy of YAMNet Model:** The reliability of the model in various environments (noise, multiple sound sources) is key. May require tuning detection logic (thresholds, timing). Misclassification (e.g., TV audio as music) is possible.
*   **Battery Consumption:** Continuous audio processing is battery-intensive. Consider implementing duty cycling to reduce power usage.
*   **Model Size:** The YAMNet model is approximately 4MB, which adds to the app size but is relatively small for a neural network model.
*   **Latency:** There may be some delay between actual playing start/stop and detection by the classifier.
*   **Permissions:** User consent for microphone and foreground service is critical.
*   **User Experience:** Clear feedback on when detection is active; reliable start/stop mechanism.

## Implementation Details

1.  **Service Structure:** `PracticeTrackingService` is implemented as a foreground service with a persistent notification.
2.  **Detection Process:**
    *   Initialize the TensorFlow Lite `AudioClassifier` with the YAMNet model.
    *   Create an `AudioRecord` instance to capture audio from the microphone.
    *   Schedule periodic processing (every 500ms) to classify the current audio.
    *   Filter results to identify music-related sounds with a confidence threshold of 0.6.
3.  **Status Updates:** Use `DetectionStateHolder` to communicate the current detection status to the UI.

## Next Steps

1.  **Refine Detection Logic:** Fine-tune thresholds and categories for better accuracy.
2.  **Implement Practice Timer:** Add logic to track and persist practice session durations.
3.  **Improve Notification:** Add dynamic text and controls to the foreground service notification.
4.  **UI Integration:** Enhance the user interface to display practice stats and current status.
5.  **Testing:** Thoroughly test the detection accuracy in different scenarios (quiet room, noisy room, different types of music/instruments).

## Technical Goals Implementation Plan

### 1. Data Model Changes

#### Create a TechnicalGoal.kt data class:
```kotlin
data class TechnicalGoal(
    val id: String = System.currentTimeMillis().toString(),
    val description: String,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val achievedDate: LocalDateTime? = null,
    val isAchieved: Boolean = false
)
```

#### Update PracticeSession.kt to include technical goals:
- Add a list of goals targeted for each session
- Add methods to manage goals for the session

### 2. ViewModel Enhancements

#### Modify PracticeViewModel.kt to:
- Add state flows for:
  - All technical goals (achieved and not achieved)
  - Outstanding goals (not achieved)
  - Current session's targeted goals
- Add methods to:
  - Create new technical goals
  - Mark goals as achieved/not achieved
  - Select goals for a session
  - Persist goals to SharedPreferences (similar to sessions)
  - Load goals from SharedPreferences

### 3. UI Components

#### Create Technical Goals List Screen:
- List view of all goals with filtering options (all/outstanding/achieved)
- Ability to add new goals with text input
- Ability to mark goals as achieved/not achieved

#### Create Goal Selection Dialog:
- When starting a session, show a dialog with outstanding goals
- Allow users to select which goals to target for the session
- Include a way to add new goals from this dialog

#### Update Session End Flow:
- After ending a session, show a dialog with the session's targeted goals
- Allow the user to mark which goals were achieved
- Update goal status accordingly

#### Update Session Details:
- Show targeted and achieved goals for each completed session in the session list

### 4. Storage

#### Enhance SharedPreferences Storage:
- Add methods to save/load technical goals alongside sessions
- Maintain relationships between sessions and their targeted goals

### 5. Implementation Steps

1. Create the TechnicalGoal data class
2. Update PracticeViewModel to support goal management
3. Implement goal persistence in SharedPreferences
4. Create the UI for managing goals (add/view/mark as achieved)
5. Implement goal selection at session start
6. Implement goal achievement confirmation at session end
7. Update session details to show goal information
8. Add goal filtering and navigation in the app

### 6. Testing Plan

- Unit tests for goal persistence and retrieval
- UI tests for goal management
- Integration tests for the session workflow with goals 