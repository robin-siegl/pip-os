package dev.pipos.launcher

import android.graphics.Bitmap
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test

/**
 * Captures the real Compose UI on an emulator for docs/screenshots.
 * Separate from smoke tests because this intentionally writes PNG assets.
 */
class ScreenshotCaptureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun captureReadmeScreenshots() {
        // The launcher briefly overlays a boot sequence before normal UI.
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("INITIALIZING PERSONAL TERMINAL_")
                .fetchSemanticsNodes().isEmpty()
        }
        rule.waitForIdle()
        val skip = rule.onAllNodesWithText("NOT NOW")
        if (skip.fetchSemanticsNodes().isNotEmpty()) {
            skip.onFirst().performClick()
        }

        capture("apps.png")
        rule.onNodeWithText("EDIT").performClick()
        capture("apps-edit.png")
        rule.onNodeWithText("DONE").performClick()

        rule.onNodeWithText("STAT").performClick()
        capture("stat.png")

        rule.onNodeWithText("CFG").performClick()
        capture("settings.png")
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        // Allow page animations and the short terminal boot overlay to settle.
        Thread.sleep(900)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
            "Failed to capture screenshot $name"
        }
        try {
            val directory = File(
                requireNotNull(instrumentation.targetContext.getExternalFilesDir(null)),
                "screenshots"
            )
            check(directory.exists() || directory.mkdirs())
            FileOutputStream(File(directory, name)).use { output ->
                check(image.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            image.recycle()
        }
    }
}
