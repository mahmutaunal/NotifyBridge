package com.alpware.notifybridge.network

import android.content.Context
import com.alpware.notifybridge.core.MacConnectionStore
import java.net.HttpURLConnection
import java.net.URL


/**
 * Performs lightweight connectivity checks against the paired Mac device.
 */
object ConnectionHealthClient {

    /**
     * Verifies whether the paired Mac server is currently reachable.
     */
    fun check(
        context: Context,
        onResult: (Boolean) -> Unit
    ) {
        Thread {
            runCatching {
                // Load the currently paired Mac connection details.
                val macIp = MacConnectionStore.getMacIp(context)
                val macPort = MacConnectionStore.getMacPort(context)

                if (macIp.isBlank()) {
                    false
                } else {
                    // Call the lightweight health endpoint exposed by the Mac app.
                    val url = URL("http://$macIp:$macPort/health")
                    val connection = url.openConnection() as HttpURLConnection

                    connection.requestMethod = "GET"
                    connection.connectTimeout = 1500
                    connection.readTimeout = 1500

                    // Treat HTTP 200 as a healthy and reachable connection state.
                    val success = connection.responseCode == 200
                    connection.disconnect()

                    success
                }
            }.onSuccess {
                onResult(it)
            }.onFailure {
                // Any network or parsing failure is treated as offline.
                onResult(false)
            }
        }.start()
    }
}