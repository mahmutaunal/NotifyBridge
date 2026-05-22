package com.alpware.notifybridge.notification

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import android.util.Log
import com.alpware.notifybridge.core.AppFilterStore
import com.alpware.notifybridge.core.BridgeStateStore
import com.alpware.notifybridge.core.PrivacyStore
import com.alpware.notifybridge.network.NotificationSender
import java.io.ByteArrayOutputStream
import androidx.core.graphics.createBitmap
import com.alpware.notifybridge.R

/**
 * Listens for Android notifications and forwards eligible ones to the paired Mac device.
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

        if (!AppFilterStore.isAppAllowed(this, sbn.packageName)) {
            Log.d(TAG, "App filtered. Notification ignored: ${sbn.packageName}")
            return
        }

        // Extract the user-visible notification content from the system bundle.
        val extras = sbn.notification.extras

        // Respect the user's privacy preference before exposing notification content.
        val showContent = PrivacyStore.shouldShowNotificationContent(this)

        val title = if (showContent) {
            extras.getCharSequence("android.title")?.toString()
        } else {
            getString(R.string.notification_hidden_title)
        }

        val text = if (showContent) {
            extras.getCharSequence("android.text")?.toString()
        } else {
            getString(R.string.notification_hidden_text)
        }

        // Resolve the application icon for future rich-notification support.
        val appIconBase64 = getAppIconBase64(sbn.packageName)

        val payload = NotificationPayload(
            packageName = sbn.packageName,
            appName = getAppName(sbn.packageName),
            title = title,
            text = text,
            postTime = sbn.postTime,
            contentHidden = !showContent,
            appIconBase64 = appIconBase64,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
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

    /**
     * Converts the app icon into a Base64 encoded PNG string.
     */
    private fun getAppIconBase64(packageName: String): String? {
        return runCatching {
            // Render the app icon into a bitmap before encoding it.
            val drawable = packageManager.getApplicationIcon(packageName)
            val bitmap = drawable.toBitmap(96, 96)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)

            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    /**
     * Renders a drawable into a bitmap with the requested dimensions.
     */
    private fun Drawable.toBitmap(width: Int, height: Int): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)

        return bitmap
    }

    companion object {
        // Shared log tag used for notification listener events.
        private const val TAG = "NotifyBridgeListener"
    }
}