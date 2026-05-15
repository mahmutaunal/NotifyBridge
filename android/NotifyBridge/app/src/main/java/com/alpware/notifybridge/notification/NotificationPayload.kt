package com.alpware.notifybridge.notification

/**
 * Represents a normalized Android notification before encryption and network transfer.
 */
data class NotificationPayload(
    val packageName: String,
    val appName: String?,
    val title: String?,
    val text: String?,
    val postTime: Long
)