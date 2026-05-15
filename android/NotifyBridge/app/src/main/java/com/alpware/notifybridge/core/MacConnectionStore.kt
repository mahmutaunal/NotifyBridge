package com.alpware.notifybridge.core

import android.content.Context
import androidx.core.content.edit

/**
 * Stores the paired Mac connection details used for local notification forwarding.
 */
object MacConnectionStore {

    private const val PREF_NAME = "notify_bridge_prefs"
    private const val KEY_MAC_IP = "mac_ip"
    private const val KEY_MAC_PORT = "mac_port"
    private const val KEY_PAIRING_TOKEN = "pairing_token"
    private const val KEY_MAC_NAME = "mac_name"

    /**
     * Returns the last paired Mac IP address.
     */
    fun getMacIp(context: Context): String {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MAC_IP, "") ?: ""
    }

    /**
     * Persists the paired Mac IP address.
     */
    fun setMacIp(context: Context, ip: String) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_MAC_IP, ip)
            }
    }

    fun getMacPort(context: Context): String {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MAC_PORT, "8787") ?: "8787"
    }

    fun setMacPort(context: Context, port: String) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_MAC_PORT, port)
            }
    }

    fun getMacName(context: Context): String {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MAC_NAME, "") ?: ""
    }

    fun setMacName(context: Context, name: String) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_MAC_NAME, name)
            }
    }

    /**
     * Returns the shared secret generated during QR pairing.
     */
    fun getPairingToken(context: Context): String {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PAIRING_TOKEN, "") ?: ""
    }

    /**
     * Persists the shared secret used for request signing and encryption.
     */
    fun setPairingToken(context: Context, token: String) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_PAIRING_TOKEN, token)
            }
    }
}