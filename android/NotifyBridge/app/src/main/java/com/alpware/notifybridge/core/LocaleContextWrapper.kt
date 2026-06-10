package com.alpware.notifybridge.core

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale


/**
 * Creates a localized context using the user's selected application language.
 */
object LocaleContextWrapper {

    /**
     * Returns a context configured with the requested locale.
     */
    fun wrap(
        context: Context,
        languageTag: String?
    ): ContextWrapper {
        if (languageTag.isNullOrBlank()) {
            return ContextWrapper(context)
        }

        // Apply the selected language to both locale and layout direction.
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        // Create a context instance backed by the localized configuration.
        val localizedContext = context.createConfigurationContext(configuration)

        return ContextWrapper(localizedContext)
    }
}