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

This plan outlines the steps to implement the Technical Goals feature as specified in FEATURES.md.

### 1. Data Model Changes

#### Create a TechnicalGoal.kt data class:
- **File:** `app/src/main/java/com/example/practicecompanion/data/TechnicalGoal.kt` (or similar path in your data layer)
```kotlin
data class TechnicalGoal(
    val id: String = UUID.randomUUID().toString(), // Use UUID for uniqueness
    val description: String,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val lastModifiedDate: LocalDateTime = LocalDateTime.now(), // For editing
    val achievedDate: LocalDateTime? = null,
    val isAchieved: Boolean = false
)
```
*   **Note:** Using UUID for `id` ensures better uniqueness than `System.currentTimeMillis()`. Added `lastModifiedDate`.

#### Update PracticeSession.kt to include technical goals:
- **File:** `app/src/main/java/com/example/practicecompanion/data/PracticeSession.kt` (or similar path)
- Add a `List<String>` property, e.g., `targetedGoalIds`, to store the IDs of goals selected for this session.

### 2. ViewModel Enhancements

#### Modify PracticeViewModel.kt (or create a dedicated GoalsViewModel):
- **File:** `app/src/main/java/com/example/practicecompanion/ui/practice/PracticeViewModel.kt` (if adding to existing) OR `app/src/main/java/com/example/practicecompanion/ui/goals/GoalsViewModel.kt` (if creating new). Creating a dedicated `GoalsViewModel` is likely cleaner.
- Add `StateFlow` or `LiveData` for goal lists (all, outstanding, session-targeted).
- Implement public functions for CRUD operations (create, edit description, delete, toggle achievement), goal selection for sessions, and handling duplicates. These functions will interact with the storage layer (Repository/DAO).

### 3. UI Components

#### Define Navigation:
- **File:** `app/src/main/java/com/example/practicecompanion/ui/navigation/AppNavigation.kt` (or your navigation graph XML/composable structure).
- Add a new route/destination for the `Technical Goals List Screen`. Decide how to link it (e.g., add a button to the main screen/bottom navigation bar, add an item in a settings menu).

#### Create Technical Goals List Screen:
- **Files:**
    - **Composable:** `app/src/main/java/com/example/practicecompanion/ui/goals/GoalsScreen.kt` (containing the main composable function, e.g., `GoalsScreen`).
    - **ViewModel:** `GoalsViewModel.kt` (as mentioned above).
- Implement using Jetpack Compose:
    - Use a `LazyColumn` to display the list of goals.
    - Include filtering controls (e.g., `FilterChip` or `TabRow`).
    - Add a `FloatingActionButton` or `Button` to trigger adding new goals.
    - Each item should allow editing (e.g., opens a dialog/new screen) and deleting (e.g., swipe-to-delete or button with confirmation dialog).
    - Allow toggling `isAchieved` status (e.g., `Checkbox` or `Switch` on each item).

#### Create Goal Creation/Editing UI:
- **Files:**
    - **Composable:** Could be a `Dialog` composable within `GoalsScreen.kt` or a separate screen `app/src/main/java/com/example/practicecompanion/ui/goals/EditGoalScreen.kt`.
- Implement a `TextField` for the description and `Button`s for Save/Cancel.

#### Create Goal Selection Dialog:
- **File:** `app/src/main/java/com/example/practicecompanion/ui/practice/PracticeScreen.kt` (or wherever the session start logic resides).
- Implement as a `Dialog` composable shown before starting the session timer.
- Display a list of outstanding goals (from `PracticeViewModel` or `GoalsViewModel`) with checkboxes.
- Include an "Add New Goal" button linking to the goal creation UI.

#### Update Session End Flow:
- **File:** `app/src/main/java/com/example/practicecompanion/ui/practice/PracticeScreen.kt` (or wherever the session end logic resides).
- Implement as a `Dialog` composable shown when the session ends.
- Display the `targetedGoalIds` for the *just-completed* session.
- Allow marking them as achieved using checkboxes or buttons.
- Call the appropriate ViewModel function to update the status in storage.

#### Update Session Details View:
- **File:** `app/src/main/java/com/example/practicecompanion/ui/history/SessionDetailScreen.kt` (or within the `SessionListScreen.kt` item).
- Fetch the actual `TechnicalGoal` objects based on the `targetedGoalIds` stored in the `PracticeSession`.
- Display the descriptions of targeted goals.
- Indicate which were achieved during that session (this might require storing achieved status *per session* or cross-referencing the `achievedDate` of the goal).

### 4. Storage

#### Choose Persistence Strategy:

- **Option A: SharedPreferences:**
    - **File:** `app/src/main/java/com/example/practicecompanion/data/AppPreferencesRepository.kt` (or similar).
    - Implement methods to save/load the list of `TechnicalGoal` objects, likely by serializing/deserializing to/from JSON using a library like `kotlinx.serialization` or Gson. Manage updates and deletions carefully.

- **Option B: Room Database (Recommended):**
    - **Files:**
        - **Entity:** `app/src/main/java/com/example/practicecompanion/data/db/GoalEntity.kt` (Annotated version of `TechnicalGoal` or a dedicated entity class).
        - **DAO:** `app/src/main/java/com/example/practicecompanion/data/db/GoalDao.kt` (Interface with `@Dao` annotation and methods for CRUD operations - insert, update, delete, query).
        - **Database:** `app/src/main/java/com/example/practicecompanion/data/db/AppDatabase.kt` (Abstract class extending `RoomDatabase`). Update to include the `GoalEntity`.
        - **Repository:** `app/src/main/java/com/example/practicecompanion/data/GoalsRepository.kt` (Optional, but recommended layer to abstract data source access from ViewModel).
    - Define `@Entity` for goals.
    - Define `@Dao` interface with `suspend` functions for DB operations (returning `Flow` for reactive updates is common).
    - Update the `AppDatabase` definition.
    - Implement a repository that uses the `GoalDao`.
    - **Relationship:** To link sessions and goals, either add `List<String> targetedGoalIds` to the `SessionEntity` OR create a separate relationship table (`SessionGoalCrossRef`). Storing IDs in the session entity is often simpler for this use case.

### 5. Implementation Steps

(These steps reference the files mentioned above)

1.  **Choose Storage:** Decide and set up (basic `AppPreferencesRepository` or Room `Entity`, `DAO`, `Database` updates).
2.  **Data Model:** Create/update `.kt` files in the `data` package.
3.  **ViewModel:** Create/update `.kt` file(s) in the `viewmodel` or `ui` package.
4.  **Storage Implementation:** Implement persistence logic in the Repository/DAO/Preferences file.
5.  **UI - Goal Management:** Create `GoalsScreen.kt` and related composables/dialogs. Update navigation structure.
6.  **UI - Session Start:** Implement dialog in `PracticeScreen.kt`.
7.  **UI - Session End:** Implement dialog in `PracticeScreen.kt`.
8.  **UI - Session Details:** Update composables in `history` package.
9.  **Integration:** Connect UI event handlers (`onClick`, etc.) to ViewModel functions. Ensure ViewModels correctly call Repository/DAO methods.

### 6. Testing Plan

- **Unit Tests:** Place in `app/src/test/java/...` corresponding package structure. Test ViewModels (using `TestCoroutineDispatcher`, mock repository), Repositories (mock DAO/Preferences), DAOs (using Room's in-memory database testing), utility functions.
- **UI/Integration Tests:** Place in `app/src/androidTest/java/...`. Use Compose testing APIs (`createComposeRule`, `onNodeWithText`, `performClick`, etc.) and potentially Hilt/Espresso for testing navigation and full flows. 