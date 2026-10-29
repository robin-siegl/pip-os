package dev.pipos.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pipos.launcher.data.AppRepository
import dev.pipos.launcher.data.FavoritesRepository
import dev.pipos.launcher.data.LaunchableApp
import dev.pipos.launcher.data.LauncherPreferences
import dev.pipos.launcher.data.LauncherPreferencesRepository
import dev.pipos.launcher.data.ThemeProfile
import dev.pipos.launcher.data.FavoriteLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = LauncherPreferencesRepository(application)
    private val _preferences = MutableStateFlow(LauncherPreferences())
    val preferences: StateFlow<LauncherPreferences> = _preferences.asStateFlow()
    private val appsRepository = AppRepository(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val _apps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val apps: StateFlow<List<LaunchableApp>> = _apps.asStateFlow()
    private val _favorites = MutableStateFlow<List<String>>(emptyList())
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refreshApps()
        viewModelScope.launch { favoritesRepository.favorites.collect { _favorites.value = it } }
        viewModelScope.launch { settingsRepository.preferences.collect { _preferences.value = it } }
    }

    fun refreshApps() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) { appsRepository.installedApps() }
        }
    }

    fun toggleFavorite(app: LaunchableApp) {
        viewModelScope.launch { favoritesRepository.toggle(app.key) }
    }

    fun moveFavorite(dragged: String, target: String) {
        viewModelScope.launch { favoritesRepository.moveByKey(dragged, target) }
    }

    fun launch(app: LaunchableApp) {
        if (!appsRepository.launch(app)) _error.value = "Unable to launch ${app.label}"
    }

    fun setTheme(profile: ThemeProfile) { viewModelScope.launch { settingsRepository.setTheme(profile) } }
    fun setEffects(enabled: Boolean) { viewModelScope.launch { settingsRepository.setEffects(enabled) } }
    fun setReduceMotion(enabled: Boolean) { viewModelScope.launch { settingsRepository.setReduceMotion(enabled) } }
    fun setFavoriteLayout(layout: FavoriteLayout) { viewModelScope.launch { settingsRepository.setFavoriteLayout(layout) } }
    fun hideApp(app: LaunchableApp) { viewModelScope.launch { settingsRepository.hide(app.key) } }
    fun clearHidden() { viewModelScope.launch { settingsRepository.clearHidden() } }
    fun addCommand(command: String) { viewModelScope.launch { settingsRepository.addCommand(command) } }

    fun clearError() { _error.value = null }
}
