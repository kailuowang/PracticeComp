# Practice Companion App Features

This document outlines the planned and potential features for the Practice Companion app.

## Core Features [MVP]

*   [x] **Diary of Practice:**  
    *  [x] **Start:**  User can start a practice session
        *  [x] **Automatic Practice Log:** As a background process, listens to instrument sound and automatically log the duration of each playing
    *  [x] **Accumulative play time display:** Display the accumulative play time for the current session in real time
    *  [x] **End:**  User can end a practice session 
        *   [x] **Automatic End** After music stops for more than 20 minutes, the app should end the practice session and retroactively set the end time to when the music stops. 
    *  [x] **List of Practice Sessions:**  For each session, display the total wall clock time and play time. 
    *  [x] **Smart Pause Detection:** Continues tracking practice time during brief pauses (less than 8 seconds) to accommodate page turns, brief rests, etc.
    *  [x] **Background Session Tracking:** Practice sessions continue tracking even when the app is minimized or the screen is off, allowing for uninterrupted practice.
    *  [x] **Session Resume:** When a session is running in the background, users can easily return to it from the main screen via a banner notification.
    *  [x] **Dairy Calendar:** Display a Month Calendar, default to current month, but can navigate to older month, on each date, display the total practice time for that day, then also display a total practice time for the month and a total life time practice time. 
        *  [x] **Unit Tests:** Comprehensive test coverage including:
            *  [x] Unit tests for month calendar utilities (first day index, weeks in month)
            *  [x] Unit tests for practice duration calculations by date/month
            *  [x] UI tests for calendar display and navigation
    *  [x] **Customizable Settings:** A dedicated settings screen where users can customize important practice detection parameters:
        *  [x] **Grace Period:** Configure how long to wait (1-60 seconds) before stopping the timer after music detection stops
        *  [x] **Auto-End Threshold:** Adjust how long of silence (1-120 minutes) before automatically ending a session

*   [x] **Session Time Goal** in the ongoing session screen, user can set a practice time goal, the value should be remembered as default for the next session. When practice time reach the goal, the app should make a "You did it!" voice. 
        * [x] **Time reminder** Whenever play time progressed by 25% of the session time goal, the app should say "Good progress! X minutes left." X stands for the minutes left for completing the session goal. 

## 1.1 Features
*   [ ] **Technical Goals** Technical goals are different from time goals, they are specific goals that the user set to achieve during the practice session, for examples, "memorize bar 24 to 36", or "improve the phrasing in page 3 line 3". 
         *  [ ] User should be able to add/remove these technical goals at any time.
         *  [ ] User should be able to set/unset goals as achieved at any time. 
         *  [ ] Every time when user starts a session, if there are outstanding (not achieved) goals, display the list and let the user choose ones to be the targets of the session
         *  [ ] After the practice session ends (either the time goal is reached or when the user select to end the session), display the targted goals and let the user select which goals are achieved. Achieved goals are no longer outstanding goals. 
         *  [ ] Keep a history of all achieved goals so that user can review later. 


## Potential Future Features

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