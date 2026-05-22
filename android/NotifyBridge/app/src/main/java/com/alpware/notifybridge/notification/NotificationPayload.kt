package com.alpware.notifybridge.notification

/**
 * Represents a normalized Android notification before encryption and network transfer.
 */
data class NotificationPayload(
    val packageName: String,
    val appName: String?,
    val title: String?,
    val text: String?,
    val postTime: Long,
    val contentHidden: Boolean = false,
    val appIconBase64: String? = null,
    val deviceName: String? = null
)