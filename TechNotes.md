# Technical Notes: Music Play Time Detection

This document outlines the technical approach and considerations for implementing the automatic detection of music playing time in a background process.

## High-Level Approach (Prioritizing SoundClassifier)

1.  **Background Service:** A foreground service is required to reliably capture and process audio when the app is not in the foreground. This service will display a persistent notification.
2.  **Audio Capture:** Utilize Android's `AudioRecord` API to access the microphone input stream.
3.  **Audio Classification (Primary Method):**
    *   Use Android's `SoundClassifier` API (part of `android.media`).
    *   Feed audio buffers captured by `AudioRecord` into the `AudioClassifier`.
    *   The classifier will use a system-provided (or potentially a custom, if needed later) model to identify sound events.
    *   Monitor the classification results for the **"Music"** category.
4.  **Detection Logic:**
    *   Define a confidence threshold for the "Music" classification.
    *   Implement logic to determine when music playing starts (e.g., "Music" category consistently above threshold for X seconds) and stops (e.g., drops below threshold or another category dominates for Y seconds).
5.  **Fallback/Alternative (If SoundClassifier is insufficient):**
    *   **Signal Processing:** Use a library like TarsosDSP to analyze audio features directly (e.g., volume changes, spectral characteristics, onset detection) and build custom logic to identify musical activity.
6.  **Logging:** When music playing is detected (via the chosen method), start/stop timers to log the duration accurately.

## Key APIs and Libraries (Prioritized)

*   **Core:**
    *   `android.app.Service` / `android.app.ForegroundService`
    *   `android.media.AudioRecord`
    *   `android.media.SoundClassifier` / `AudioClassifier`
    *   `android.Manifest.permission.RECORD_AUDIO`
    *   `android.Manifest.permission.FOREGROUND_SERVICE`
    *   `android.Manifest.permission.POST_NOTIFICATIONS` (Android 13+)
*   **Potential Fallback:**
    *   TarsosDSP (for signal processing features if `SoundClassifier` fails)

## Challenges and Considerations

*   **Accuracy of `SoundClassifier`:** The reliability of the system's "Music" model in various environments (noise, multiple sound sources) is key. May require tuning detection logic (thresholds, timing). Misclassification (e.g., TV audio as music) is possible.
*   **Battery Consumption:** Continuous audio processing is battery-intensive. `SoundClassifier` might offer some system optimizations, but monitoring and potential duty cycling strategies are still important.
*   **API Level Requirements:** `SoundClassifier` works best on newer Android versions (S/API 31+). Consider compatibility and potential use of support libraries if targeting older versions.
*   **Latency:** Delay between actual playing start/stop and detection by the classifier.
*   **Permissions:** User consent for microphone and foreground service is critical.
*   **User Experience:** Clear feedback on when detection is active; reliable start/stop mechanism.

## Next Steps

1.  **Implement Foreground Service:** Set up the basic structure for a foreground service that requests necessary permissions.
2.  **Integrate `AudioRecord`:** Add code to capture raw audio data within the service.
3.  **Integrate `SoundClassifier`:** Instantiate `AudioClassifier` and start feeding it audio data.
4.  **Develop Detection Logic:** Implement the logic to analyze `SoundClassifier` results for the "Music" category, applying thresholds and timing to trigger practice time logging.
5.  **Testing:** Thoroughly test the detection accuracy in different scenarios (quiet room, noisy room, different types of music/instruments).
6.  **Evaluate:** If accuracy is insufficient, investigate the fallback signal processing approach using TarsosDSP. 