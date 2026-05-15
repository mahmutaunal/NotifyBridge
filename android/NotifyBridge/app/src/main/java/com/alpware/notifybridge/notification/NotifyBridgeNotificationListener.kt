package com.alpware.notifybridge.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.alpware.notifybridge.core.BridgeStateStore
import com.alpware.notifybridge.network.NotificationSender

/**
 * Listens for Android notifications and forwards them to the paired Mac device.
 */
class NotifyBridgeNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }

    /**
     * Called whenever a new Android notification is posted to the system.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val isBridgeEnabled = BridgeStateStore.isBridgeEnabled(this)

        if (!isBridgeEnabled) {
            Log.d(TAG, "Bridge disabled. Notification ignored: ${sbn.packageName}")
            return
        }

        // Extract the user-visible notification content from the system bundle.
        val extras = sbn.notification.extras

        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()

        val payload = NotificationPayload(
            packageName = sbn.packageName,
            appName = getAppName(sbn.packageName),
            title = title,
            text = text,
            postTime = sbn.postTime
        )

        Log.d(TAG, "Bridge enabled. Notification received: $payload")

        NotificationSender.send(this, payload)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "Notification removed: ${sbn.packageName}")
    }

    /**
     * Resolves the human-readable application name from the package manager.
     */
    private fun getAppName(packageName: String): String? {
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrNull()
    }

    companion object {
        private const val TAG = "NotifyBridgeListener"
    }
}