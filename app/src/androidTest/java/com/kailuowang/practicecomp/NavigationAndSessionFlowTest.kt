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
    private val practiceSessionTitlePattern = "Practice Session" // Partial match for the title which includes version
    private val backButtonDesc = "Back"
    private val practiceListPlaceholderText = "No practice sessions yet" // Partial text match

    @Test
    fun navigate_fromList_toSession_andBack() {
        // 1. Wait for the initial List screen to be ready and verify placeholder
        composeTestRule.waitUntil(timeoutMillis = 5000) {
             composeTestRule
                .onAllNodesWithText(practiceListPlaceholderText, substring = true) // Use substring match
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(practiceListPlaceholderText, substring = true).assertIsDisplayed()
        // composeTestRule.waitForIdle() // Potentially redundant after waitUntil

        // 2. Click FAB
        composeTestRule.onNodeWithContentDescription(startPracticeButtonDesc).performClick()
        composeTestRule.waitForIdle() // Wait after click for navigation

        // 3. Wait for Session screen to display and verify title
        composeTestRule.waitUntil(timeoutMillis = 5000) {
             composeTestRule
                .onAllNodesWithText(practiceSessionTitlePattern, substring = true) // Use substring match
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(practiceSessionTitlePattern, substring = true).assertIsDisplayed()
        // composeTestRule.waitForIdle() // Potentially redundant after waitUntil

        // 4. Click Back button
        composeTestRule.onNodeWithContentDescription(backButtonDesc).performClick()
        composeTestRule.waitForIdle() // Wait after click for navigation

        // 5. Wait for List screen to reappear and verify placeholder
        composeTestRule.waitUntil(timeoutMillis = 10000) { // Increased timeout
             composeTestRule
                .onAllNodesWithText(practiceListPlaceholderText, substring = true) // Use substring match
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(practiceListPlaceholderText, substring = true).assertIsDisplayed()
    }
}