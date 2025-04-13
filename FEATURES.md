# Practice Companion App Features

This document outlines the planned and potential features for the Practice Companion app.

## Core Features (MVP)

*   [x] **Diary of Practice:**  
    *  [x] **Start:**  User can start a practice session
        *  [x] **Automatic Practice Log:** As a background process, listens to instrument sound and automatically log the duration of each playing
    *  [x] **Accumulative play time display:** Display the accumulative play time for the current session in real time
    *  [x] **End:**  User can end a practice session 
        *  [ ] **Automatic End** when the music has stopped for more than 20 minutes, the session automatically ends and set the session end time retroactively to when the music stopped.
    *  [x] **List of Practice Sessions:**  For each session, display the total wall clock time and play time. 
    *  [x] **Smart Pause Detection:** Continues tracking practice time during brief pauses (less than 8 seconds) to accommodate page turns, brief rests, etc.
    *  [x] **Background Session Tracking:** Practice sessions continue tracking even when the app is minimized or the screen is off, allowing for uninterrupted practice.
    *  [x] **Session Resume:** When a session is running in the background, users can easily return to it from the main screen via a banner notification.
    *  [x] **Dairy Calendar:** Display a Month Calendar, default to current month, but can navigate to older month, on each date, display the total practice time for that day, then also display a total practice time for the month and a total life time practice time. 
        *  [x] **Unit Tests:** Comprehensive test coverage including:
            *  [x] Unit tests for month calendar utilities (first day index, weeks in month)
            *  [x] Unit tests for practice duration calculations by date/month
            *  [x] UI tests for calendar display and navigation

*   [x] **Session Time Goal** in the ongoing session screen, user can set a practice time goal, the value should be remembered as default for the next session. When practice time reach the goal, the app should make a "You did it!" voice. 
        * [x] **Time reminder** Whenever play time progressed by 25% of the session time goal, the app should say "Good progress! X minutes left." X stands for the minutes left for completing the session goal. 


## Potential Future Features
*   [ ] **Session Technical Goals** Technical goals are different from time goals, they are specific goals that the user set to achieve during the practice session, for examples, "memorize bar 24 to 36", or "improve the phrasing in page 3 line 3". User should be able to add such goals during a session through voice command. Here is how. First when the music stops, and when speech is detected, app should do a voice to text to monitor what the user is saying, if the user says "Add a goal", the app will display a text input and start to display the text from the user's voice. If the user starts playing, or silent for 10 seconds, save the text as a session goal and assign it with a goal number, plays a voice confirmation with the new goal number. If the user says "cancel", then cancel the action. Technical goals should be displayed in the session screen. 
*   [ ] **Metronome:** A configurable metronome with tempo, time signature, and sound options.
*   [ ] **Sheet Music Viewer:** Display sheet music files (e.g., PDF, MusicXML).
*   [ ] **Goal Setting:** Allow users to set practice goals (e.g., time per week, specific pieces).
*   [ ] **Progress Tracking:** Visualize practice time and achievements.
*   [ ] **Integration with Music Theory:** Quizzes or exercises related to theory.
*   [ ] **Customizable Exercises:** Allow users to create or import practice exercises.
*   [ ] **Cloud Sync:** Sync practice logs and settings across devices.

## Ideas Under Consideration

*   [ ] **Recording:** Basic audio recording capability for self-assessment.
*   [ ] **Scale/Arpeggio Library:** Visual and auditory reference for scales and arpeggios.
*   [ ] Gamification elements (streaks, points).
*   [ ] AI-powered feedback on intonation/rhythm. 
*   [ ] **Enhanced Notification Controls:** Add ability to control the practice session (pause/resume/end) directly from the notification.
*   [ ] **Practice Stats Dashboard:** Detailed analytics of practice habits, including time of day, duration patterns, and consistency metrics. 