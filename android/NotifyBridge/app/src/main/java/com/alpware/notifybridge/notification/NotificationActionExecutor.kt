package com.alpware.notifybridge.notification

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.util.Log
import com.alpware.notifybridge.network.NotificationActionCommand
import com.alpware.notifybridge.network.NotificationRegistry

/**
 * Executes notification actions received from the paired Mac device.
 */
class NotificationActionExecutor(
    private val service: NotificationListenerService
) {

    /**
     * Routes an incoming notification action command to the appropriate handler.
     */
    fun execute(command: NotificationActionCommand) {
        when (command.type) {
            "dismiss" -> dismiss(command)
            "openOnPhone" -> openOnPhone(command)
            "reply" -> reply(command)
            else -> Log.d(TAG, "Unknown notification action: ${command.type}")
        }
    }

    /**
     * Dismisses an active notification on the Android device.
     */
    private fun dismiss(
        command: NotificationActionCommand
    ) {
        val key = command.notificationKey

        if (key.isNullOrBlank()) {
            Log.d(TAG, "Dismiss ignored. Empty key.")
            return
        }

        val sbn = NotificationRegistry.get(key)

        if (sbn == null) {
            Log.d(TAG, "Dismiss ignored. Notification not found.")
            return
        }

        runCatching {
            service.cancelNotification(key)
            NotificationRegistry.remove(key)
            Log.d(TAG, "Notification dismissed: $key")
        }.onFailure {
            Log.e(
                TAG,
                "Dismiss failed: $key",
                it
            )
        }
    }

    /**
     * Opens the related application on the Android device.
     */
    private fun openOnPhone(command: NotificationActionCommand) {
        val packageName = command.packageName

        if (packageName.isNullOrBlank()) {
            Log.d(TAG, "Open on phone ignored. Package name is empty.")
            return
        }

        runCatching {
            // Resolve the app launch intent from the provided package name.
            val intent = service.packageManager
                .getLaunchIntentForPackage(packageName)

            if (intent == null) {
                Log.d(TAG, "Open on phone ignored. Launch intent not found: $packageName")
                return
            }

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )

            service.startActivity(intent)

            Log.d(TAG, "Opened app on phone: $packageName")
        }.onFailure {
            Log.e(TAG, "Open on phone failed: $packageName", it)
        }
    }

    /**
     * Sends a direct reply through a notification RemoteInput action.
     */
    private fun reply(command: NotificationActionCommand) {
        val key = command.notificationKey
        if (key.isNullOrBlank()) {
            Log.d(TAG, "Reply ignored. Notification key is empty.")
            return
        }

        val replyText = command.replyText
        if (replyText.isNullOrBlank()) {
            Log.d(TAG, "Reply ignored. Reply text is empty.")
            return
        }

        val sbn = NotificationRegistry.get(key)
        if (sbn == null) {
            Log.d(TAG, "Reply ignored. Notification no longer exists: $key")
            return
        }

        // Search notification actions for a compatible RemoteInput target.
        val actions = sbn.notification.actions
        if (actions.isNullOrEmpty()) {
            Log.d(TAG, "Reply ignored. Notification has no actions: $key")
            return
        }

        actions.forEach { action ->
            val remoteInputs = action.remoteInputs
            if (remoteInputs.isNullOrEmpty()) return@forEach

            runCatching {
                // Build a RemoteInput payload containing the reply text.
                val intent = Intent()
                val bundle = Bundle()

                remoteInputs.forEach { remoteInput ->
                    bundle.putCharSequence(
                        remoteInput.resultKey,
                        replyText
                    )
                }

                RemoteInput.addResultsToIntent(
                    remoteInputs,
                    intent,
                    bundle
                )

                // Deliver the reply through the notification action PendingIntent.
                action.actionIntent.send(
                    service,
                    0,
                    intent
                )

                Log.d(TAG, "Reply sent successfully: $key")
                return

            }.onFailure {
                Log.e(TAG, "Reply failed: $key", it)
            }
        }

        Log.d(TAG, "Reply ignored. No RemoteInput action found: $key")
    }

    /**
     * Shared log tag used for notification action execution.
     */
    companion object {
        private const val TAG = "NotificationActionExecutor"
    }
}