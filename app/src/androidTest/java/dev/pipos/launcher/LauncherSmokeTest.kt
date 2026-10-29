package dev.pipos.launcher

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.pipos.launcher.data.FavoritesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Runs on an emulator or physical Android device. */
class LauncherSmokeTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Before fun dismissFirstRunScreen() {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("INITIALIZING PERSONAL TERMINAL_")
                .fetchSemanticsNodes().isEmpty()
        }
        val notNow = composeRule.onAllNodesWithText("NOT NOW")
        if (notNow.fetchSemanticsNodes().isNotEmpty()) notNow.onFirst().performClick()
    }

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
        composeRule.onNodeWithText("CHANGE HOME APP").assertExists()
    }

    @Test fun pinningFirstAppUpdatesPinnedListWithoutTabSwitch() {
        // Keep this independent of test order and any previously saved favorites.
        val repository = FavoritesRepository(composeRule.activity)
        runBlocking {
            repository.favorites.first().forEach { repository.toggle(it) }
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("pinned-shortcuts").fetchSemanticsNodes().isEmpty()
        }
        val unpinned = composeRule.onAllNodes(hasContentDescription("Pin ", substring = true))
        composeRule.waitUntil(10_000) { unpinned.fetchSemanticsNodes().isNotEmpty() }
        unpinned.onFirst().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("pinned-shortcuts").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("pinned-shortcuts").assertExists()
        composeRule.onNodeWithText("APPS").assertExists()
    }
}
