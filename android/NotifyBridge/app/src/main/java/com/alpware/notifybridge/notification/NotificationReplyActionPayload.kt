package com.alpware.notifybridge.notification


/**
 * Describes a notification reply action that can be executed remotely.
 */
data class NotificationReplyActionPayload(
    val actionIndex: Int,
    val label: String?,
    val resultKey: String
)