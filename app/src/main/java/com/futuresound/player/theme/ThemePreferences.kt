package com.futuresound.player.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

/**
 * Persists the user's selected theme id across app restarts.
 */
class ThemePreferences(private val context: Context) {

    private val themeIdKey = intPreferencesKey("selected_theme_id")

    val selectedThemeId: Flow<Int> = context.themeDataStore.data.map { prefs ->
        prefs[themeIdKey] ?: DEFAULT_THEME_ID
    }

    suspend fun setSelectedTheme(themeId: Int) {
        context.themeDataStore.edit { prefs ->
            prefs[themeIdKey] = themeId
        }
    }

    companion object {
        const val DEFAULT_THEME_ID = 0
    }
}
