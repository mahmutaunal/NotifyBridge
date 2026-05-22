package com.alpware.notifybridge.core

import android.content.Context
import androidx.core.content.edit

/**
 * Stores and manages notification forwarding preferences for installed apps.
 */
object AppFilterStore {

    private const val PREF_NAME = "notify_bridge_app_filters"
    private const val KEY_SEND_ALL_APPS = "send_all_apps"
    private const val KEY_SELECTED_PACKAGES = "selected_packages"

    /**
     * Returns whether all installed apps should forward notifications.
     */
    fun shouldSendAllApps(context: Context): Boolean {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SEND_ALL_APPS, true)
    }

    /**
     * Enables or disables global forwarding for all installed apps.
     */
    fun setSendAllApps(context: Context, enabled: Boolean) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_SEND_ALL_APPS, enabled)
            }
    }

    /**
     * Returns the set of app package names individually allowed for forwarding.
     */
    fun getSelectedPackages(context: Context): Set<String> {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_SELECTED_PACKAGES, emptySet())
            ?: emptySet()
    }

    /**
     * Enables or disables forwarding for a specific application package.
     */
    fun setAppEnabled(
        context: Context,
        packageName: String,
        enabled: Boolean
    ) {
        val current = getSelectedPackages(context).toMutableSet()

        if (enabled) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }

        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putStringSet(KEY_SELECTED_PACKAGES, current)
            }
    }

    /**
     * Clears all individually selected applications.
     */
    fun clearSelectedApps(context: Context) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putStringSet(KEY_SELECTED_PACKAGES, emptySet())
            }
    }

    /**
     * Returns whether notifications from the given app package should be forwarded.
     */
    fun isAppAllowed(context: Context, packageName: String): Boolean {
        if (shouldSendAllApps(context)) {
            return true
        }

        return packageName in getSelectedPackages(context)
    }
}