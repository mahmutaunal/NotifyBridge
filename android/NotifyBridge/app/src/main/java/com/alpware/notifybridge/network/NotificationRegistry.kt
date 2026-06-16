package com.alpware.notifybridge.network

import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentHashMap

/**
 * Maintains active Android notifications that can later receive action commands.
 */
object NotificationRegistry {

    private val notifications =
        ConcurrentHashMap<String, StatusBarNotification>()

    /**
     * Stores a notification using its system notification key.
     */
    fun put(sbn: StatusBarNotification) {
        notifications[sbn.key] = sbn
    }

    /**
     * Returns the notification associated with the provided key.
     */
    fun get(key: String): StatusBarNotification? {
        return notifications[key]
    }

    /**
     * Removes a notification from the registry when it is dismissed or no longer needed.
     */
    fun remove(key: String) {
        notifications.remove(key)
    }
}