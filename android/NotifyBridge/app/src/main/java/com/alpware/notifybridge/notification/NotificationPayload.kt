package com.alpware.notifybridge.notification

/**
 * Represents a normalized Android notification before encryption and network transfer.
 */
data class NotificationPayload(
    val protocolVersion: Int = 2,
    val historyId: String = java.util.UUID.randomUUID().toString(),
    val eventType: String = "notification_upsert",
    val deviceId: String? = null,
    val packageName: String,
    val appName: String?,
    val title: String?,
    val text: String?,
    val postTime: Long,
    val contentHidden: Boolean = false,
    val appIconBase64: String? = null,
    val deviceName: String? = null,
    val notificationKey: String? = null,
    val canDismiss: Boolean = true,
    val canOpenOnPhone: Boolean = true,
    val canReply: Boolean = false,
    val replyAction: NotificationReplyActionPayload? = null
)