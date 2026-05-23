package com.alpware.notifybridge.core

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceNameResolver {

    /**
     * Resolves the user-defined Android device name shown in system settings.
     */
    fun get(context: Context): String {

        val settingsName = runCatching {
            Settings.Global.getString(
                context.contentResolver,
                Settings.Global.DEVICE_NAME
            )
        }.getOrNull()

        if (!settingsName.isNullOrBlank()) {
            return settingsName
        }

        val bluetoothName = runCatching {
            Settings.Secure.getString(
                context.contentResolver,
                "bluetooth_name"
            )
        }.getOrNull()

        if (!bluetoothName.isNullOrBlank()) {
            return bluetoothName
        }

        return Build.MODEL
    }
}