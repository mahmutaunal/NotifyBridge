package com.alpware.notifybridge.network

import android.content.Context
import com.alpware.notifybridge.core.PairedMacStore
import com.alpware.notifybridge.core.AndroidDeviceIdentity

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
                val selected = PairedMacStore.getSelected(context)
                val macIp = selected?.host.orEmpty()
                val token = selected?.secret.orEmpty()

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
                connection.setRequestProperty("X-NotifyBridge-Device-Id", AndroidDeviceIdentity.get(context))

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