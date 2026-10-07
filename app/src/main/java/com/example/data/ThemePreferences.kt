package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class NoteViewMode {
    LIST,
    CARD
}

enum class NoteSection {
    ALL,
    LOCKED,
    TRASH
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _viewMode = MutableStateFlow(loadViewMode())
    val viewMode: StateFlow<NoteViewMode> = _viewMode.asStateFlow()

    private val _lockPin = MutableStateFlow(loadLockPin())
    val lockPin: StateFlow<String?> = _lockPin.asStateFlow()

    private fun loadThemeMode(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name)
        return try {
            ThemeMode.valueOf(saved ?: ThemeMode.DARK.name)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    private fun loadViewMode(): NoteViewMode {
        val saved = prefs.getString(KEY_VIEW_MODE, NoteViewMode.LIST.name)
        return try {
            NoteViewMode.valueOf(saved ?: NoteViewMode.LIST.name)
        } catch (e: Exception) {
            NoteViewMode.LIST
        }
    }

    private fun loadLockPin(): String? {
        return prefs.getString(KEY_LOCK_PIN, null)?.takeIf { it.isNotBlank() }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setViewMode(mode: NoteViewMode) {
        prefs.edit().putString(KEY_VIEW_MODE, mode.name).apply()
        _viewMode.value = mode
    }

    fun setLockPin(pin: String) {
        val cleaned = pin.trim()
        prefs.edit().putString(KEY_LOCK_PIN, cleaned).apply()
        _lockPin.value = cleaned
    }

    fun verifyLockPin(inputPin: String): Boolean {
        val current = _lockPin.value
        return current != null && current == inputPin.trim()
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_VIEW_MODE = "key_view_mode"
        private const val KEY_LOCK_PIN = "key_lock_pin"
    }
}
