package com.alpware.notifybridge.network

import android.content.Context
import com.alpware.notifybridge.core.PairedMacStore
import com.alpware.notifybridge.core.AndroidDeviceIdentity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Retrieves pending notification action commands from the paired Mac device.
 */
object NotificationActionClient {

    /**
     * Fetches notification actions waiting to be executed on Android.
     */
    fun fetchPendingActions(
        context: Context,
        onResult: (Result<List<NotificationActionCommand>>) -> Unit
    ) {
        Thread {
            runCatching {
                // Use the pairing token to authenticate the action request.
                val token = PairedMacStore.getSelected(context)?.secret.orEmpty()

                if (token.isBlank()) {
                    return@runCatching emptyList<NotificationActionCommand>()
                }

                // Connect through the pinned TLS channel established during pairing.
                val connection = PinnedHttpsClient.openConnection(
                    context = context,
                    path = "/actions",
                    connectTimeout = 1500,
                    readTimeout = 1500
                )

                connection.requestMethod = "GET"
                connection.setRequestProperty("X-NotifyBridge-Token", token)
                connection.setRequestProperty("X-NotifyBridge-Device-Id", AndroidDeviceIdentity.get(context))

                // Ensure the server accepted and processed the request.
                val responseCode = connection.responseCode

                if (responseCode != 200) {
                    connection.disconnect()
                    error("Action fetch failed. HTTP $responseCode")
                }

                // Read the serialized list of pending notification actions.
                val responseText = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                // Deserialize the JSON response into action command models.
                val type = object : TypeToken<List<NotificationActionCommand>>() {}.type
                Gson().fromJson(responseText, type)
            }.onSuccess {
                onResult(Result.success(it))
            }.onFailure {
                onResult(Result.failure(it))
            }
        }.start()
    }
}