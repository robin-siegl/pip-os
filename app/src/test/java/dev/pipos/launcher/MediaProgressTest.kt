package dev.pipos.launcher

import dev.pipos.launcher.media.MediaProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaProgressTest {
    @Test fun interpolatesOnlyWhenPlaying() {
        assertEquals(7_000L, MediaProgress.position(5_000, 1_000, 3_000, 1f, true, 10_000))
        assertEquals(5_000L, MediaProgress.position(5_000, 1_000, 3_000, 1f, false, 10_000))
    }

    @Test fun clampsToDurationAndHandlesMissingDuration() {
        assertEquals(10_000L, MediaProgress.position(9_000, 1_000, 6_000, 1f, true, 10_000))
        assertEquals(0L, MediaProgress.position(9_000, 1_000, 6_000, 1f, true, 0))
        assertEquals(5_000L, MediaProgress.position(5_000, 3_000, 2_000, 1f, true, 10_000))
    }

    @Test fun rendersDurations() {
        assertEquals("0:00", MediaProgress.timeCode(-300))
        assertEquals("2:03", MediaProgress.timeCode(123_900))
    }
}
