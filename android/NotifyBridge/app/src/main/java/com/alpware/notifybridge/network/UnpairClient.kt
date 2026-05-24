package com.alpware.notifybridge.network

import android.content.Context
import com.alpware.notifybridge.core.MacConnectionStore
import java.net.HttpURLConnection
import java.net.URL


/**
 * Notifies the paired Mac app that the Android device is disconnecting.
 */
object UnpairClient {

    /**
     * Sends an unpair request to the paired Mac device.
     */
    fun unpair(
        context: Context,
        onComplete: () -> Unit = {}
    ) {
        Thread {
            runCatching {
                // Load the current pairing information from local storage.
                val macIp = MacConnectionStore.getMacIp(context)
                val macPort = MacConnectionStore.getMacPort(context)
                val token = MacConnectionStore.getPairingToken(context)

                // Skip the request if pairing information is incomplete.
                if (macIp.isBlank() || token.isBlank()) {
                    return@runCatching
                }

                // Call the Mac unpair endpoint to invalidate the active session.
                val url = URL("http://$macIp:$macPort/unpair")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 1500
                connection.readTimeout = 1500
                connection.doOutput = false
                connection.setRequestProperty("X-NotifyBridge-Token", token)

                // Trigger the HTTP request even though the response body is ignored.
                connection.responseCode
                connection.disconnect()
            }
            // Always notify the caller when the background operation finishes.
            onComplete()
        }.start()
    }
}