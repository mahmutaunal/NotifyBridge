package com.alpware.notifybridge.core

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.alpware.notifybridge.ui.AppLanguageMode


/**
 * Applies the application's runtime language configuration.
 */
object AppLanguageManager {

    /**
     * Updates the app locale based on the selected language mode.
     */
    fun apply(mode: AppLanguageMode) {
        // Map the selected language mode to the corresponding locale list.
        val locales = when (mode) {
            AppLanguageMode.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguageMode.TURKISH -> LocaleListCompat.forLanguageTags("tr")
            AppLanguageMode.ENGLISH -> LocaleListCompat.forLanguageTags("en")
        }

        // Apply the locale change across the entire application.
        AppCompatDelegate.setApplicationLocales(locales)
    }
}