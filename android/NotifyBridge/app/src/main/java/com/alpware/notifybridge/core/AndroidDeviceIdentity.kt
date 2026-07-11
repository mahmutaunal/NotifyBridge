package com.alpware.notifybridge.core

import android.content.Context
import java.util.UUID

object AndroidDeviceIdentity {
    private const val KEY = "android_device_id_v2"
    fun get(context: Context): String {
        val existing = SecureStringStore.getString(context, KEY)
        if (existing.isNotBlank()) return existing
        return UUID.randomUUID().toString().also { SecureStringStore.putString(context, KEY, it) }
    }
}
