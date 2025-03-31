package com.kailuowang.practicecomp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Rule

/**
 * Instrumented tests for the main navigation flow.
 */
@RunWith(AndroidJUnit4::class)
class NavigationAndSessionFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Constants for UI elements
    private val startPracticeButtonDesc = "Start new practice session"
    private val practiceSessionTitle = "Practice Session" // Assuming this title exists
    private val backButtonDesc = "Back"
    private val practiceListPlaceholderText = "Practice sessions will appear here."

    @Test
    fun navigate_fromList_toSession_andBack() {
        // 1. Start on List screen, verify placeholder
        composeTestRule.onNodeWithText(practiceListPlaceholderText).assertIsDisplayed()

        // 2. Click FAB
        composeTestRule.onNodeWithContentDescription(startPracticeButtonDesc).performClick()

        // 3. Verify Session screen is displayed (check title)
        composeTestRule.onNodeWithText(practiceSessionTitle).assertIsDisplayed()

        // 4. Click Back button
        composeTestRule.onNodeWithContentDescription(backButtonDesc).performClick()

        // 5. Verify back on List screen
        composeTestRule.onNodeWithText(practiceListPlaceholderText).assertIsDisplayed()
    }
}