package dev.pipos.launcher

import dev.pipos.launcher.data.LaunchableApp
import dev.pipos.launcher.ui.filterApps
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTest {
    private val apps = listOf(
        LaunchableApp("Camera", "com.android.camera", "Main"),
        LaunchableApp("Music", "com.example.music", "Main")
    )

    @Test fun blankQueryReturnsAll() = assertEquals(apps, filterApps(apps, ""))
    @Test fun searchesCaseInsensitively() = assertEquals(listOf(apps[0]), filterApps(apps, "CAM"))
    @Test fun supportsTerminalOpenPrefix() = assertEquals(listOf(apps[1]), filterApps(apps, ">open music"))
    @Test fun searchesPackageNames() = assertEquals(listOf(apps[1]), filterApps(apps, "example"))
    @Test fun unknownAppReturnsEmpty() = assertEquals(emptyList<LaunchableApp>(), filterApps(apps, "unknown"))
}
