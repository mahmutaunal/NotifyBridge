package com.alpware.notifybridge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.alpware.notifybridge.MainActivity
import com.alpware.notifybridge.R
import com.alpware.notifybridge.core.BridgeStateStore
import com.alpware.notifybridge.network.ConnectionRecoveryManager

/** Keeps the bridge alive and actively restores its LAN endpoint when connectivity changes. */
class BridgeForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var connectivityManager: ConnectivityManager
    private var callbackRegistered = false

    private val recoveryRunnable = object : Runnable {
        override fun run() {
            if (BridgeStateStore.isBridgeEnabled(this@BridgeForegroundService)) {
                ConnectionRecoveryManager.recover(this@BridgeForegroundService)
                handler.postDelayed(this, RECOVERY_INTERVAL_MS)
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = scheduleImmediateRecovery()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                scheduleImmediateRecovery()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        scheduleImmediateRecovery()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (callbackRegistered) runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        callbackRegistered = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleImmediateRecovery() {
        handler.removeCallbacks(recoveryRunnable)
        handler.postDelayed(recoveryRunnable, NETWORK_SETTLE_DELAY_MS)
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { connectivityManager.registerNetworkCallback(request, networkCallback) }
            .onSuccess { callbackRegistered = true }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify_bridge_tile)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_message))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.foreground_channel_name), NotificationManager.IMPORTANCE_LOW
        ).apply { description = getString(R.string.foreground_channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "notifybridge_service"
        private const val NOTIFICATION_ID = 1001
        private const val NETWORK_SETTLE_DELAY_MS = 750L
        private const val RECOVERY_INTERVAL_MS = 30_000L
    }
}
