package dev.pipos.launcher

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** Runs on an emulator or physical Android device. */
class LauncherSmokeTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun showsPrimaryNavigationAndSearch() {
        composeRule.onAllNodesWithText("PIP-OS").onFirst().assertExists()
        composeRule.onNodeWithText("APPS").assertExists()
        composeRule.onNodeWithContentDescription("Search apps or enter a terminal command").assertExists()
    }

    @Test fun opensStatisticsScreen() {
        composeRule.onNodeWithText("STAT").performClick()
        composeRule.onNodeWithText("SYSTEM STATUS // LIVE TELEMETRY").assertExists()
    }

    @Test fun opensSettings() {
        composeRule.onNodeWithText("CFG").performClick()
        composeRule.onNodeWithText("CONFIGURATION // DISPLAY").assertExists()
    }
}
