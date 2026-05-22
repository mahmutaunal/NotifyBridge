package com.alpware.notifybridge.core

import android.content.Context
import androidx.core.content.edit

/**
 * Persists privacy-related user preferences for notification content visibility.
 */
object PrivacyStore {

    private const val PREF_NAME = "notify_bridge_privacy"
    private const val KEY_SHOW_NOTIFICATION_CONTENT = "show_notification_content"

    /**
     * Returns whether notification content should be displayed on the paired Mac device.
     */
    fun shouldShowNotificationContent(context: Context): Boolean {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_NOTIFICATION_CONTENT, true)
    }

    /**
     * Updates the notification content visibility preference.
     */
    fun setShowNotificationContent(context: Context, enabled: Boolean) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_SHOW_NOTIFICATION_CONTENT, enabled)
            }
    }
}