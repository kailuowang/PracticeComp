package com.kailuowang.practicecomp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * Verifies that the main activity launches and displays the expected initial screen.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    // Rule to launch MainActivity before each test
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun practiceListScreen_displaysCorrectly() {
        // Check if the placeholder text is displayed
        composeTestRule.onNodeWithText("Practice sessions will appear here.").assertIsDisplayed()

        // Optionally, we could also check for the title or the FAB
        // composeTestRule.onNodeWithText("Practice Diary").assertIsDisplayed()
        // composeTestRule.onNodeWithContentDescription("Start new practice session").assertIsDisplayed()
    }
}