package com.futuresound.player.theme

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Holds the currently active theme as observable state and persists changes.
 * This is the single thing the rest of the app needs to know about theming.
 */
class ThemeController(
    context: Context,
    private val scope: CoroutineScope
) {
    private val preferences = ThemePreferences(context)

    private val _activeTheme = MutableStateFlow(ThemeGenerator.byId(ThemePreferences.DEFAULT_THEME_ID))
    val activeTheme: StateFlow<FutureTheme> = _activeTheme.asStateFlow()

    init {
        preferences.selectedThemeId
            .onEach { id -> _activeTheme.value = ThemeGenerator.byId(id) }
            .launchIn(scope)
    }

    fun selectTheme(themeId: Int) {
        scope.launch {
            preferences.setSelectedTheme(themeId)
        }
    }

    fun allThemes(): List<FutureTheme> = ThemeGenerator.all()
}
