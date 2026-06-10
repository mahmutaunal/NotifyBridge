package com.alpware.notifybridge.service

import android.content.Context
import android.content.Intent

/**
 * Centralizes starting and stopping the foreground bridge service.
 */
object BridgeServiceController {

    /**
     * Starts the persistent foreground service responsible for background forwarding.
     */
    fun start(context: Context) {
        runCatching {
            val intent = Intent(context, BridgeForegroundService::class.java)
            context.startForegroundService(intent)
        }
    }

    /**
     * Stops the foreground bridge service and removes its notification.
     */
    fun stop(context: Context) {
        val intent = Intent(context, BridgeForegroundService::class.java)
        context.stopService(intent)
    }
}