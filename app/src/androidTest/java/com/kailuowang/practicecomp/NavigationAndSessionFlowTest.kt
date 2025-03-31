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
    private val practiceListPlaceholderText = "Practice sessions will appear here."

    @Test
    fun navigate_fromList_toSession_andBack() {
        // 1. Wait for the initial List screen to be ready and verify placeholder
        composeTestRule.waitUntil(timeoutMillis = 5000) {
             composeTestRule
                .onAllNodesWithText(practiceListPlaceholderText)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(practiceListPlaceholderText).assertIsDisplayed()
        // composeTestRule.waitForIdle() // Potentially redundant after waitUntil

        // 2. Click FAB
        composeTestRule.onNodeWithContentDescription(startPracticeButtonDesc).performClick()
        composeTestRule.waitForIdle() // Wait after click for navigation

        // 3. Wait for Session screen to display and verify title
        composeTestRule.waitUntil(timeoutMillis = 5000) {
             composeTestRule
                .onAllNodesWithText(practiceSessionTitle)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(practiceSessionTitle).assertIsDisplayed()
        // composeTestRule.waitForIdle() // Potentially redundant after waitUntil

        // 4. Click Back button
        composeTestRule.onNodeWithContentDescription(backButtonDesc).performClick()
        composeTestRule.waitForIdle() // Wait after click for navigation

        // 5. Wait for List screen to reappear and verify placeholder
        composeTestRule.waitUntil(timeoutMillis = 5000) {
             composeTestRule
                .onAllNodesWithText(practiceListPlaceholderText)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(practiceListPlaceholderText).assertIsDisplayed()
    }
}