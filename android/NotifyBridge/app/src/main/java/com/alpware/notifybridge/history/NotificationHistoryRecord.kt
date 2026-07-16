package com.alpware.notifybridge.history

data class NotificationHistoryRecord(
    val historyId: String,
    val sourceKey: String,
    val deviceId: String,
    val packageName: String,
    val appName: String?,
    val title: String?,
    val text: String?,
    val postedAt: Long,
    val updatedAt: Long,
    val removedAt: Long?,
    val lifecycleState: String,
    val deliveryState: String,
    val deliveryAttemptCount: Int,
    val deliveredAt: Long?,
    val contentHidden: Boolean,
    val canDismiss: Boolean,
    val canOpenOnPhone: Boolean,
    val canReply: Boolean
)
