package com.alpware.notifybridge.network

import com.google.gson.annotations.SerializedName

/**
 * Represents a notification action requested by the paired Mac device.
 */
data class NotificationActionCommand(
    @SerializedName("id")
    val id: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("notificationKey")
    val notificationKey: String?,

    @SerializedName("packageName")
    val packageName: String?,

    @SerializedName("replyText")
    val replyText: String?,

    @SerializedName("createdAt")
    val createdAt: String?
)