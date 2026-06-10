package com.alpware.notifybridge.core

import android.content.Context
import androidx.core.content.edit
import com.alpware.notifybridge.ui.AppThemeMode

/**
 * Persists and retrieves the user's theme preference.
 */
object ThemeStore {

    private const val PREF_NAME = "notify_bridge_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    /**
     * Returns the currently selected application theme mode.
     */
    fun getThemeMode(context: Context): AppThemeMode {
        val value = context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)

        return runCatching {
            AppThemeMode.valueOf(value ?: AppThemeMode.SYSTEM.name)
        }.getOrDefault(AppThemeMode.SYSTEM)
    }

    /**
     * Stores the selected application theme mode.
     */
    fun setThemeMode(context: Context, mode: AppThemeMode) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_THEME_MODE, mode.name)
            }
    }
}