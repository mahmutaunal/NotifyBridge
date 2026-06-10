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
    private const val KEY_MAC_CERT_FINGERPRINT = "mac_cert_fingerprint"

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

    fun getPairingToken(context: Context): String {
        return SecureStringStore.getString(context, KEY_PAIRING_TOKEN)
    }

    fun setPairingToken(context: Context, token: String) {
        SecureStringStore.putString(context, KEY_PAIRING_TOKEN, token)
    }

    fun getMacCertFingerprint(context: Context): String {
        return SecureStringStore.getString(context, KEY_MAC_CERT_FINGERPRINT)
    }

    fun setMacCertFingerprint(context: Context, fingerprint: String) {
        SecureStringStore.putString(context, KEY_MAC_CERT_FINGERPRINT, fingerprint)
    }
}