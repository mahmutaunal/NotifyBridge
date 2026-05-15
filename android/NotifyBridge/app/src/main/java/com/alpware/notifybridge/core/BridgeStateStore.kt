package com.alpware.notifybridge.core

import android.content.Context
import androidx.core.content.edit

/**
 * Persists the notification forwarding state shared across the Android app.
 */
object BridgeStateStore {

    private const val PREF_NAME = "notify_bridge_prefs"
    private const val KEY_BRIDGE_ENABLED = "bridge_enabled"

    /**
     * Returns whether notification forwarding is currently enabled.
     */
    fun isBridgeEnabled(context: Context): Boolean {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BRIDGE_ENABLED, false)
    }

    /**
     * Updates the persisted notification forwarding state.
     */
    fun setBridgeEnabled(context: Context, enabled: Boolean) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_BRIDGE_ENABLED, enabled)
            }
    }
}