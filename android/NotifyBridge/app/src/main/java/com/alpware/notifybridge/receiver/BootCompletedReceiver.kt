package com.alpware.notifybridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alpware.notifybridge.core.BridgeStateStore
import com.alpware.notifybridge.service.BridgeServiceController

/**
 * Restarts the foreground bridge service after device boot when forwarding is enabled.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    /**
     * Handles system boot events and restores the notification bridge state.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // Ignore unrelated broadcasts received by the receiver.
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        val bridgeEnabled = BridgeStateStore.isBridgeEnabled(context)

        if (!bridgeEnabled) {
            Log.d(TAG, "Bridge disabled. Service not started after boot.")
            return
        }

        Log.d(TAG, "Bridge enabled. Starting foreground service after boot.")
        BridgeServiceController.start(context)
    }

    companion object {
        private const val TAG = "NotifyBridgeBoot"
    }
}