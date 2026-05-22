package com.alpware.notifybridge.model


/**
 * Represents an installed Android app displayed in the notification filter list.
 */
data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean
)