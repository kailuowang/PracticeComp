// package com.kailuowang.practicecomp
// 
// import androidx.compose.ui.test.*
// import androidx.compose.ui.test.junit4.createAndroidComposeRule
// import androidx.test.ext.junit.runners.AndroidJUnit4
// import androidx.test.platform.app.InstrumentationRegistry
// import org.junit.Before
// import org.junit.Rule
// import org.junit.Test
// import org.junit.runner.RunWith
// 
// /**
//  * Instrumented tests for verifying the UI interaction with the PracticeTrackingService.
//  * THESE TESTS ARE OBSOLETE as the Start/Stop buttons have been removed.
//  */
// @RunWith(AndroidJUnit4::class)
// class PracticeTrackingServiceTest {
// 
//     @get:Rule
//     val composeTestRule = createAndroidComposeRule<MainActivity>()
// 
//     // UI Element Locators
//     private val startPracticeButtonDesc = "Start new practice session"
//     private val startTrackingButtonText = "Start Tracking"
//     private val stopTrackingButtonText = "Stop Tracking"
// 
//     @Before
//     fun navigateToSessionScreen() {
//         // Ensure we are on the correct screen before each test
//         composeTestRule.onNodeWithContentDescription(startPracticeButtonDesc).performClick()
//         // Simple wait after click
//         composeTestRule.waitForIdle()
//     }
// 
//     // Note: Granting permissions automatically in tests is complex.
//     // These tests assume permissions are either pre-granted or handled manually during the test run.
//     // A more robust solution might involve UIAutomator to click permission dialogs.
// 
//     @Test
//     fun clickingStartTracking_showsStopTrackingButton() {
//         // Wait for the target screen to load
//         composeTestRule.waitUntil(timeoutMillis = 5_000) {
//             runCatching { composeTestRule.onNodeWithText(startTrackingButtonText).assertIsDisplayed() }.isSuccess
//         }
// 
//         // 1. Verify Start button is initially visible
//         composeTestRule.onNodeWithText(startTrackingButtonText).assertIsDisplayed()
//         composeTestRule.onNodeWithText(stopTrackingButtonText).assertDoesNotExist()
// 
//         // 2. Click Start Tracking
//         composeTestRule.onNodeWithText(startTrackingButtonText).performClick()
// 
//         // 3. Verify Stop button is now visible
//         // Allow time for potential permission dialogs and state update
//         composeTestRule.waitUntil(timeoutMillis = 10_000) { // Increased timeout for potential dialogs
//             runCatching { composeTestRule.onNodeWithText(stopTrackingButtonText).assertIsDisplayed() }.isSuccess
//         }
//         composeTestRule.onNodeWithText(stopTrackingButtonText).assertIsDisplayed()
//         composeTestRule.onNodeWithText(startTrackingButtonText).assertDoesNotExist()
// 
//         // Cleanup: Stop the service after the test if it started
//         try {
//            composeTestRule.onNodeWithText(stopTrackingButtonText).performClick()
//         } catch (e: AssertionError) {
//             // Ignore if the button wasn't found (e.g., permissions denied or test failed earlier)
//         } catch (e: IllegalStateException) {
//             // Ignore if hierarchy is lost during cleanup
//         }
//     }
// 
//     @Test
//     fun clickingStopTracking_showsStartTrackingButton() {
//         // Wait for the target screen to load
//         composeTestRule.waitUntil(timeoutMillis = 5_000) {
//              runCatching { composeTestRule.onNodeWithText(startTrackingButtonText).assertIsDisplayed() }.isSuccess
//         }
// 
//         // 1. Start tracking first
//         composeTestRule.onNodeWithText(startTrackingButtonText).performClick()
// 
//         // Wait until stop button appears (handling potential permission dialogs)
//         composeTestRule.waitUntil(timeoutMillis = 10_000) {
//             runCatching { composeTestRule.onNodeWithText(stopTrackingButtonText).assertIsDisplayed() }.isSuccess
//         }
//         composeTestRule.onNodeWithText(stopTrackingButtonText).assertIsDisplayed()
// 
//         // 2. Click Stop Tracking
//         composeTestRule.onNodeWithText(stopTrackingButtonText).performClick()
// 
//         // 3. Verify Start button is visible again
//         composeTestRule.waitForIdle() // Allow UI to settle after click
//         composeTestRule.onNodeWithText(startTrackingButtonText).assertIsDisplayed()
//         composeTestRule.onNodeWithText(stopTrackingButtonText).assertDoesNotExist()
//     }
// } 