package com.kailuowang.practicecomp

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.rememberNavController
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.kailuowang.practicecomp.ui.theme.PracticeCompTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for PracticeSessionScreen focusing on automatic service lifecycle.
 */
@RunWith(AndroidJUnit4::class)
class PracticeSessionScreenTest {

    // Rule to grant permissions before the test runs
    // Grant RECORD_AUDIO for all tests in this class for simplicity
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO,
        // Grant notification permission as well, required on API 33+
        Manifest.permission.POST_NOTIFICATIONS
    )

    // Use createAndroidComposeRule to access Activity context if needed,
    // or createComposeRule() if testing composables in isolation (might need more setup).
    // createAndroidComposeRule is generally easier for Activity-level interactions like permissions.
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // We don't need a separate TestNavHostController if we let the Activity handle navigation
    // private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        // No need to create TestNavHostController or setContent here.
        // The rule launches MainActivity, which sets its own content and NavHost.

        // We need to navigate from the initial screen (PracticeListScreen) 
        // to the PracticeSessionScreen within the Activity's NavHost.
        // Find the FAB (Floating Action Button) on the list screen and click it.
        composeTestRule.onNodeWithContentDescription("Start new practice session", useUnmergedTree = true)
            .performClick()
        
        // Add a small wait to ensure navigation completes (optional but can help stability)
        composeTestRule.waitForIdle()
    }

    @Test
    fun test_monitoringStartsAutomatically_whenPermissionsGranted() {
        // Permissions are granted by the @Rule before setup
        // Navigation to PracticeSessionScreen is done in @Before

        // Verify that the "Monitoring active..." text appears shortly after navigating
        composeTestRule.waitUntil(timeoutMillis = 5000) {
             composeTestRule
                .onAllNodesWithText("Monitoring active...", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Monitoring active...", ignoreCase = true).assertIsDisplayed()

        // Verify the initial status (might be "Starting..." or "Listening..." depending on timing)
        // Let's wait for "Listening..." as the service likely starts quickly
         composeTestRule.waitUntil(timeoutMillis = 5000) { 
            composeTestRule
                .onAllNodesWithText("Status: Listening...", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Status: Listening...", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // TODO: Add test for permission request flow (when permissions initially denied)
    // This requires more complex setup to deny permissions and interact with the permission dialog.

    // TODO: Add test for service stopping on navigation back / dispose
    // This might require mocking service calls or observing state changes more directly.
} 