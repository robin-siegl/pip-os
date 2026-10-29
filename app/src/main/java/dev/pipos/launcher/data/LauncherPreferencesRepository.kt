package dev.pipos.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeProfile { GREEN, AMBER, ICE }
enum class FavoriteLayout { GRID, LIST }

data class LauncherPreferences(
    val theme: ThemeProfile = ThemeProfile.GREEN,
    val scanlines: Boolean = false,
    val reduceMotion: Boolean = false,
    val favoriteLayout: FavoriteLayout = FavoriteLayout.GRID,
    val hidden: Set<String> = emptySet(),
    val commandHistory: List<String> = emptyList(),
    val homeSetupDismissed: Boolean = false
)

private val Context.launcherSettings by preferencesDataStore(name = "terminal_settings")

class LauncherPreferencesRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val scanlineKey = booleanPreferencesKey("scanlines")
    private val motionKey = booleanPreferencesKey("reduce_motion")
    private val layoutKey = stringPreferencesKey("favorites_layout")
    private val hiddenKey = stringSetPreferencesKey("hidden")
    private val historyKey = stringPreferencesKey("history")
    private val homeSetupKey = booleanPreferencesKey("home_setup_dismissed")

    val preferences: Flow<LauncherPreferences> = context.launcherSettings.data.map { data ->
        LauncherPreferences(
            theme = ThemeProfile.entries.firstOrNull { it.name == data[themeKey] } ?: ThemeProfile.GREEN,
            scanlines = data[scanlineKey] ?: false,
            reduceMotion = data[motionKey] ?: false,
            favoriteLayout = FavoriteLayout.entries.firstOrNull { it.name == data[layoutKey] } ?: FavoriteLayout.GRID,
            hidden = data[hiddenKey] ?: emptySet(),
            commandHistory = data[historyKey]?.split("\n")?.filter(String::isNotBlank) ?: emptyList(),
            homeSetupDismissed = data[homeSetupKey] ?: false
        )
    }

    suspend fun dismissHomeSetup() {
        context.launcherSettings.edit { it[homeSetupKey] = true }
    }

    suspend fun setTheme(theme: ThemeProfile) { context.launcherSettings.edit { it[themeKey] = theme.name } }
    suspend fun setEffects(enabled: Boolean) { context.launcherSettings.edit { it[scanlineKey] = enabled } }
    suspend fun setReduceMotion(enabled: Boolean) { context.launcherSettings.edit { it[motionKey] = enabled } }
    suspend fun setFavoriteLayout(layout: FavoriteLayout) { context.launcherSettings.edit { it[layoutKey] = layout.name } }
    suspend fun hide(key: String) { context.launcherSettings.edit { it[hiddenKey] = it[hiddenKey].orEmpty() + key } }
    suspend fun clearHidden() { context.launcherSettings.edit { it.remove(hiddenKey) } }
    suspend fun addCommand(command: String) {
        context.launcherSettings.edit {
            val old = it[historyKey]?.split("\n")?.filter(String::isNotBlank).orEmpty()
            it[historyKey] = (listOf(command.replace("\n", " ")) + old.filterNot { previous -> previous == command })
                .take(20).joinToString("\n")
        }
    }
}
