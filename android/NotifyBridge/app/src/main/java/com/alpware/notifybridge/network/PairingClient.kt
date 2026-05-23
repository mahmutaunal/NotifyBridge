package com.alpware.notifybridge.network

import android.content.Context
import com.alpware.notifybridge.core.DeviceNameResolver
import com.alpware.notifybridge.pairing.PairingRequest
import com.alpware.notifybridge.pairing.PairingResponse
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles the initial pairing handshake between the Android device and the Mac app.
 */
object PairingClient {

    /**
     * Sends the pairing code and device information to the Mac pairing endpoint.
     */
    fun pair(
        context: Context,
        host: String,
        port: Int,
        code: String,
        onResult: (Result<PairingResponse>) -> Unit
    ) {
        Thread {
            runCatching {
                val url = URL("http://$host:$port/pair")
                // Include a readable Android device name for the Mac pairing UI.
                val request = PairingRequest(
                    code = code,
                    deviceName = DeviceNameResolver.get(context)
                )

                // Serialize the pairing request into JSON before transmission.
                val json = Gson().toJson(request)

                // Open a direct HTTP connection to the local Mac pairing server.
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                // Send the pairing payload to the Mac application.
                val bodyBytes = json.toByteArray(Charsets.UTF_8)

                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Content-Length", bodyBytes.size.toString())
                connection.setFixedLengthStreamingMode(bodyBytes.size)

                connection.outputStream.use { output ->
                    output.write(bodyBytes)
                    output.flush()
                }

                val responseCode = connection.responseCode

                if (responseCode != 200) {
                    connection.disconnect()
                    error("Pairing failed. HTTP $responseCode")
                }
                // Parse the encrypted pairing response returned by the Mac app.
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                Gson().fromJson(responseText, PairingResponse::class.java)
            }.onSuccess {
                onResult(Result.success(it))
            }.onFailure {
                onResult(Result.failure(it))
            }
        }.start()
    }
}