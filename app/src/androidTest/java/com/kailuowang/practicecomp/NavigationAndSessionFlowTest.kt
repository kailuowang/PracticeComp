package com.kailuowang.practicecomp

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Rule

/**
 * Instrumented tests for the main navigation flow.
 */
@RunWith(AndroidJUnit4::class)
class NavigationAndSessionFlowTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Constants for UI elements
    private val startPracticeButtonDesc = "Start new practice session"
    private val practiceSessionTitle = "Practice Session" // Assuming this title exists
    private val backButtonDesc = "Back"
    private val practiceListPlaceholderText = "No practice sessions yet. Tap + to start one."
    private val practiceListTitle = "Practice Diary"

    @Test
    fun navigate_fromList_toSession_andBack() {
        // 1. Wait for the initial List screen to be ready by checking for the title
        composeTestRule.waitUntil(timeoutMillis = 20000) {
             composeTestRule
                .onAllNodesWithText(practiceListTitle)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        
        // Verify we're in the list screen
        try {
            composeTestRule.onNodeWithText(practiceListPlaceholderText).assertIsDisplayed()
        } catch (e: AssertionError) {
            // If we don't find the placeholder text, we might already have sessions
            // Just verify we're on the list screen by checking the title
            composeTestRule.onNodeWithText(practiceListTitle).assertIsDisplayed()
        }
        
        // 2. Click FAB
        composeTestRule.onNodeWithContentDescription(startPracticeButtonDesc).performClick()
        composeTestRule.waitForIdle() // Wait after click for navigation

        // 3. Wait for Session screen to display and verify title
        composeTestRule.waitUntil(timeoutMillis = 20000) {
             composeTestRule
                .onAllNodesWithText(practiceSessionTitle)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(practiceSessionTitle).assertIsDisplayed()
        
        // 4. Click Back button
        composeTestRule.onNodeWithContentDescription(backButtonDesc).performClick()
        composeTestRule.waitForIdle() // Wait after click for navigation

        // 5. Wait for List screen to reappear by checking for the title
        composeTestRule.waitUntil(timeoutMillis = 20000) {
             composeTestRule
                .onAllNodesWithText(practiceListTitle)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        
        // Then check that we're on the list screen
        composeTestRule.onNodeWithText(practiceListTitle).assertIsDisplayed()
    }
}