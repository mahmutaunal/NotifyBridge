package com.alpware.notifybridge.notification

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
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
import com.alpware.notifybridge.core.DeviceNameResolver
import com.alpware.notifybridge.core.MacConnectionStore
import com.alpware.notifybridge.network.NotificationActionClient
import com.alpware.notifybridge.network.NotificationRegistry

/**
 * Listens for Android notifications and forwards eligible ones to the paired Mac device.
 */
class NotifyBridgeNotificationListener : NotificationListenerService() {

    private val actionExecutor by lazy {
        NotificationActionExecutor(this)
    }

    // Prevents overlapping action fetch requests.
    private var isFetchingActions = false

    // Schedules periodic polling for notification actions from the Mac app.
    private val actionPollingHandler = Handler(Looper.getMainLooper())
    private var isActionPollingActive = false

    // Periodically checks for pending actions while the listener is active.
    private val actionPollingRunnable = object : Runnable {
        override fun run() {
            if (!isActionPollingActive) return

            val bridgeEnabled = BridgeStateStore.isBridgeEnabled(this@NotifyBridgeNotificationListener)
            val hasPairing = MacConnectionStore.getMacIp(this@NotifyBridgeNotificationListener).isNotBlank() &&
                    MacConnectionStore.getPairingToken(this@NotifyBridgeNotificationListener).isNotBlank()

            if (bridgeEnabled && hasPairing) {
                fetchPendingActions()
            }

            actionPollingHandler.postDelayed(this, ACTION_POLLING_INTERVAL_MS)
        }
    }

    /**
     * Starts action synchronization when notification access becomes available.
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        fetchPendingActions()
        startActionPolling()
        Log.d(TAG, "Notification listener connected")
    }

    /**
     * Stops background polling when notification access is disconnected.
     */
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        stopActionPolling()
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

        NotificationRegistry.put(sbn)

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

        val replyAction = getReplyActionPayload(sbn)

        val payload = NotificationPayload(
            packageName = sbn.packageName,
            appName = getAppName(sbn.packageName),
            title = title,
            text = text,
            postTime = sbn.postTime,
            contentHidden = !showContent,
            appIconBase64 = appIconBase64,
            deviceName = DeviceNameResolver.get(this),
            notificationKey = sbn.key,
            canDismiss = true,
            canOpenOnPhone = true,
            canReply = sbn.notification.actions?.any { action ->
                action.remoteInputs?.isNotEmpty() == true
            } == true,
            replyAction = replyAction
        )

        Log.d(TAG, "Bridge enabled. Notification received: $payload")

        NotificationSender.send(this, payload)

        fetchPendingActions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        NotificationRegistry.remove(sbn.key)
        Log.d(TAG, "Notification removed: ${sbn.packageName}")
    }

    override fun onDestroy() {
        stopActionPolling()
        super.onDestroy()
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

    /**
     * Retrieves and executes pending notification actions from the paired Mac.
     */
    private fun fetchPendingActions() {
        if (isFetchingActions) return

        isFetchingActions = true

        // Request queued actions and execute them one by one.
        NotificationActionClient.fetchPendingActions(this) { result ->
            isFetchingActions = false

            result.getOrNull()
                ?.forEach { command ->
                    actionExecutor.execute(command)
                }
        }
    }

    /**
     * Extracts the first available RemoteInput action for quick replies.
     */
    private fun getReplyActionPayload(sbn: StatusBarNotification): NotificationReplyActionPayload? {
        val actions = sbn.notification.actions ?: return null

        actions.forEachIndexed { index, action ->
            val remoteInput = action.remoteInputs?.firstOrNull() ?: return@forEachIndexed

            // Expose the reply metadata required for remote reply execution.
            return NotificationReplyActionPayload(
                actionIndex = index,
                label = action.title?.toString(),
                resultKey = remoteInput.resultKey
            )
        }

        return null
    }

    /**
     * Starts periodic polling for remote notification actions.
     */
    private fun startActionPolling() {
        if (isActionPollingActive) return

        isActionPollingActive = true
        actionPollingHandler.removeCallbacks(actionPollingRunnable)
        actionPollingHandler.post(actionPollingRunnable)
    }

    /**
     * Stops periodic polling and clears scheduled callbacks.
     */
    private fun stopActionPolling() {
        isActionPollingActive = false
        actionPollingHandler.removeCallbacks(actionPollingRunnable)
    }

    companion object {
        // Shared log tag used for notification listener events.
        private const val TAG = "NotifyBridgeListener"
        // Poll interval used to synchronize notification actions.
        private const val ACTION_POLLING_INTERVAL_MS = 15_000L
    }
}