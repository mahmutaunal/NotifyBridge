package com.alpware.notifybridge.network

import android.content.Context
import com.alpware.notifybridge.core.MacConnectionStore

/**
 * Performs lightweight connectivity checks against the paired Mac device.
 */
object ConnectionHealthClient {

    /**
     * Verifies whether the paired Mac server is currently reachable.
     */
    fun check(
        context: Context,
        onResult: (ConnectionHealthResult) -> Unit
    ) {
        Thread {
            val result = runCatching {
                val macIp = MacConnectionStore.getMacIp(context)
                val token = MacConnectionStore.getPairingToken(context)

                if (macIp.isBlank() || token.isBlank()) {
                    return@runCatching ConnectionHealthResult.PairingInvalid
                }

                val connection = PinnedHttpsClient.openConnection(
                    context = context,
                    path = "/health",
                    connectTimeout = 1500,
                    readTimeout = 1500
                )

                connection.requestMethod = "GET"
                connection.connectTimeout = 1500
                connection.readTimeout = 1500
                connection.setRequestProperty("X-NotifyBridge-Token", token)

                when (connection.responseCode) {
                    200 -> ConnectionHealthResult.Online
                    401, 403 -> ConnectionHealthResult.PairingInvalid
                    else -> ConnectionHealthResult.Offline
                }.also {
                    connection.disconnect()
                }
            }.getOrElse {
                ConnectionHealthResult.Offline
            }

            onResult(result)
        }.start()
    }
}