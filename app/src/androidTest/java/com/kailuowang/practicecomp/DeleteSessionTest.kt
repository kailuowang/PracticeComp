package com.kailuowang.practicecomp

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * UI test for the session deletion feature.
 */
@RunWith(AndroidJUnit4::class)
class DeleteSessionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Constants for UI elements
    private val deleteButtonDesc = "Delete session"
    private val deleteConfirmationTitle = "Delete Practice Session"
    private val deleteConfirmText = "Delete"
    private val cancelText = "Cancel"
    private val noSessionsText = "No practice sessions yet"

    private lateinit var viewModel: PracticeViewModel
    private lateinit var testSession: PracticeSession
    
    @Before
    fun setup() {
        // Create a test session directly in the ViewModel
        composeTestRule.activityRule.scenario.onActivity { activity ->
            viewModel = PracticeViewModel(activity.application)
            
            // Create and save a test session
            testSession = PracticeSession(
                id = "test-session-id",
                date = LocalDateTime.now(),
                totalTimeMillis = 3600000, // 1 hour
                practiceTimeMillis = 1800000 // 30 minutes
            )
            
            // Use reflection to add the session directly to avoid going through the UI flow
            val field = PracticeViewModel::class.java.getDeclaredField("_sessions")
            field.isAccessible = true
            val sessionList = mutableListOf(testSession)
            field.set(viewModel, kotlinx.coroutines.flow.MutableStateFlow(sessionList))
            
            // Save to SharedPreferences
            runBlocking {
                viewModel.refreshSessions()
            }
        }
        
        // Wait for the app to stabilize
        composeTestRule.waitForIdle()
    }
    
    @After
    fun cleanup() {
        // Clean up by deleting test data from SharedPreferences
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedPrefs = context.getSharedPreferences("PracticeCompPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()
    }

    @Test
    fun delete_session_flow_cancel() {
        // Wait for the session item to appear
        composeTestRule.waitForIdle()
        
        // 1. Find and click the delete button
        composeTestRule.onNodeWithContentDescription(deleteButtonDesc).performClick()
        
        // 2. Verify the confirmation dialog appears
        composeTestRule.onNodeWithText(deleteConfirmationTitle).assertIsDisplayed()
        
        // 3. Click cancel
        composeTestRule.onNodeWithText(cancelText).performClick()
        
        // 4. Verify the session is still displayed (not deleted)
        composeTestRule.onNodeWithText("Practice: 30:00").assertIsDisplayed()
    }
    
    @Test
    fun delete_session_flow_confirm() {
        // Wait for the session item to appear
        composeTestRule.waitForIdle()
        
        // 1. Find and click the delete button
        composeTestRule.onNodeWithContentDescription(deleteButtonDesc).performClick()
        
        // 2. Verify the confirmation dialog appears
        composeTestRule.onNodeWithText(deleteConfirmationTitle).assertIsDisplayed()
        
        // 3. Click delete to confirm
        composeTestRule.onNodeWithText(deleteConfirmText).performClick()
        
        // 4. Wait for the UI to update
        composeTestRule.waitForIdle()
        
        // 5. Verify the session is no longer displayed
        composeTestRule.onNodeWithText(noSessionsText, substring = true).assertIsDisplayed()
    }
} 