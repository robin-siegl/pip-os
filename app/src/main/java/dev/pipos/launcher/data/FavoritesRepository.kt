package dev.pipos.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesStore by preferencesDataStore(name = "launcher_preferences")

/** Keeps the original Set key to migrate users of earlier PIP-OS builds without data loss. */
class FavoritesRepository(private val context: Context) {
    private val legacyKey = stringSetPreferencesKey("favorite_activities")
    private val orderKey = stringPreferencesKey("favorite_order")

    val favorites: Flow<List<String>> = context.favoritesStore.data.map { prefs ->
        val legacy = prefs[legacyKey].orEmpty()
        val saved = prefs[orderKey]?.split('\n')?.filter(String::isNotEmpty).orEmpty()
        FavoriteOrder.reconcile(saved, legacy)
    }

    suspend fun toggle(appKey: String) {
        context.favoritesStore.edit { prefs ->
            val saved = prefs[orderKey]?.split('\n')?.filter(String::isNotEmpty).orEmpty()
            val next = FavoriteOrder.toggle(FavoriteOrder.reconcile(saved, prefs[legacyKey].orEmpty()), appKey)
            prefs[legacyKey] = next.toSet()
            prefs[orderKey] = next.joinToString("\n")
        }
    }

    suspend fun move(from: Int, to: Int) {
        context.favoritesStore.edit { prefs ->
            val saved = prefs[orderKey]?.split('\n')?.filter(String::isNotEmpty).orEmpty()
            val next = FavoriteOrder.move(FavoriteOrder.reconcile(saved, prefs[legacyKey].orEmpty()), from, to)
            prefs[legacyKey] = next.toSet()
            prefs[orderKey] = next.joinToString("\n")
        }
    }
}
