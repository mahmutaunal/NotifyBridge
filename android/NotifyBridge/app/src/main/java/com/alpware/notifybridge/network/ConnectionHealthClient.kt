package com.alpware.notifybridge.network

import android.content.Context
import com.alpware.notifybridge.core.AndroidDeviceIdentity
import com.alpware.notifybridge.core.PairedMacStore
import com.alpware.notifybridge.model.PairedMac

/** Performs lightweight authenticated connectivity checks against a paired Mac. */
object ConnectionHealthClient {

    fun check(context: Context, onResult: (ConnectionHealthResult) -> Unit) {
        val selected = PairedMacStore.getSelected(context)
        if (selected == null) {
            onResult(ConnectionHealthResult.PairingInvalid)
            return
        }
        check(context, selected, onResult)
    }

    /**
     * Checks a concrete endpoint while preserving certificate pinning. This overload is also used
     * by recovery discovery to prove that a newly resolved address is the already-paired Mac.
     */
    fun check(
        context: Context,
        mac: PairedMac,
        onResult: (ConnectionHealthResult) -> Unit
    ) {
        Thread {
            onResult(checkBlocking(context, mac))
        }.start()
    }

    internal fun checkBlocking(context: Context, mac: PairedMac): ConnectionHealthResult {
        if (!mac.isValid) return ConnectionHealthResult.PairingInvalid

        return runCatching {
            val connection = PinnedHttpsClient.openConnection(
                mac = mac,
                path = "/health",
                connectTimeout = 1800,
                readTimeout = 1800
            )
            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("X-NotifyBridge-Token", mac.secret)
                connection.setRequestProperty("X-NotifyBridge-Device-Id", AndroidDeviceIdentity.get(context))

                when (connection.responseCode) {
                    200 -> ConnectionHealthResult.Online
                    401, 403 -> ConnectionHealthResult.PairingInvalid
                    else -> ConnectionHealthResult.Offline
                }
            } finally {
                connection.disconnect()
            }
        }.getOrElse { ConnectionHealthResult.Offline }
    }
}
