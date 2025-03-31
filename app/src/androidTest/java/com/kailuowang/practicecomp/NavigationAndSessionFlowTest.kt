package com.kailuowang.practicecomp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Rule

/**
 * Instrumented tests for the main navigation flow, including instrument selection
 * and starting a session.
 */
@RunWith(AndroidJUnit4::class)
class NavigationAndSessionFlowTest {

    // Rule to launch MainActivity before each test
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Define constants for reuse
    private val startPracticeButtonDesc = "Start new practice session"
    private val selectInstrumentTitle = "Select Instrument"
    private val confirmButtonText = "Confirm"
    private val pianoText = "Piano"
    private val celloText = "Cello"
    private val sessionScreenInstrumentPrefix = "Instrument: "
    private val backButtonDesc = "Back"

    @Test
    fun fullNavigationFlow_selectPiano_displaysInSession() {
        // 1. Start on List screen, click FAB
        composeTestRule.onNodeWithContentDescription(startPracticeButtonDesc).performClick()

        // 2. Verify Instrument Selection screen is displayed
        composeTestRule.onNodeWithText(selectInstrumentTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(confirmButtonText).assertIsNotEnabled() // Confirm initially disabled

        // 3. Select Piano
        composeTestRule.onNodeWithText(pianoText).performClick()
        composeTestRule.onNodeWithText(confirmButtonText).assertIsEnabled() // Confirm now enabled

        // 4. Click Confirm
        composeTestRule.onNodeWithText(confirmButtonText).performClick()

        // 5. Verify Session screen is displayed with the correct instrument
        composeTestRule.onNodeWithText(sessionScreenInstrumentPrefix + pianoText).assertIsDisplayed()
    }

    @Test
    fun instrumentSelection_persistsAsDefault() {
        // --- First flow: Select Piano ---
        composeTestRule.onNodeWithContentDescription(startPracticeButtonDesc).performClick()
        composeTestRule.onNodeWithText(pianoText).performClick()
        composeTestRule.onNodeWithText(confirmButtonText).performClick()
        // Go back to list screen
        composeTestRule.onNodeWithContentDescription(backButtonDesc).performClick()

        // --- Second flow: Check default ---
        composeTestRule.onNodeWithContentDescription(startPracticeButtonDesc).performClick()

        // Verify Piano is pre-selected on Instrument Selection screen
        composeTestRule.onNodeWithText(selectInstrumentTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(pianoText).assertIsSelected() // Check if radio button is selected
        composeTestRule.onNodeWithText(celloText).assertIsNotSelected()
        composeTestRule.onNodeWithText(confirmButtonText).assertIsEnabled() // Confirm should be enabled due to pre-selection
    }

     @Test
    fun instrumentSelection_backButton_resetsSelection() {
        // --- First flow: Select Piano ---
        composeTestRule.onNodeWithContentDescription(startPracticeButtonDesc).performClick()
        composeTestRule.onNodeWithText(pianoText).performClick()
        composeTestRule.onNodeWithText(confirmButtonText).performClick()
        // Go back to list screen
        composeTestRule.onNodeWithContentDescription(backButtonDesc).performClick()

        // --- Second flow: Start selection, select Cello, press Back ---
        composeTestRule.onNodeWithContentDescription(startPracticeButtonDesc).performClick()
        // Piano should be pre-selected
        composeTestRule.onNodeWithText(pianoText).assertIsSelected()
        // Select Cello temporarily
        composeTestRule.onNodeWithText(celloText).performClick()
        composeTestRule.onNodeWithText(celloText).assertIsSelected()
        // Press Back
        composeTestRule.onNodeWithContentDescription(backButtonDesc).performClick()

        // --- Third flow: Verify default is still Piano ---
        composeTestRule.onNodeWithContentDescription(startPracticeButtonDesc).performClick()
        composeTestRule.onNodeWithText(selectInstrumentTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(pianoText).assertIsSelected() // Piano should still be the default
        composeTestRule.onNodeWithText(celloText).assertIsNotSelected()
    }

    // Keep the original test to ensure the list screen still loads initially
    @Test
    fun practiceListScreen_displaysCorrectly() {
        composeTestRule.onNodeWithText("Practice sessions will appear here.").assertIsDisplayed()
    }
}