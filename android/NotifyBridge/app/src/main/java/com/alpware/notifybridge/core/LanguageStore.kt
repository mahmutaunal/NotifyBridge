package com.alpware.notifybridge.core

import android.content.Context
import androidx.core.content.edit
import com.alpware.notifybridge.ui.AppLanguageMode

/**
 * Persists and retrieves the user's language preference.
 */
object LanguageStore {

    private const val PREF_NAME = "notify_bridge_prefs"
    private const val KEY_LANGUAGE_MODE = "language_mode"

    /**
     * Returns the currently selected application language mode.
     */
    fun getLanguageMode(context: Context): AppLanguageMode {
        val value = context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_MODE, AppLanguageMode.SYSTEM.name)

        // Fallback to the system language if the stored value is invalid.
        return runCatching {
            AppLanguageMode.valueOf(value ?: AppLanguageMode.SYSTEM.name)
        }.getOrDefault(AppLanguageMode.SYSTEM)
    }

    /**
     * Stores the selected application language mode.
     */
    fun setLanguageMode(context: Context, mode: AppLanguageMode) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_LANGUAGE_MODE, mode.name)
            }
    }
}