package com.alpware.notifybridge.network

import android.content.Context
import com.alpware.notifybridge.core.DeviceNameResolver
import com.alpware.notifybridge.core.AndroidDeviceIdentity
import com.alpware.notifybridge.pairing.PairingRequest
import com.alpware.notifybridge.pairing.PairingResponse
import com.google.gson.Gson
import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

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
        fingerprint: String,
        onResult: (Result<PairingResponse>) -> Unit
    ) {
        Thread {
            runCatching {
                val url = URL("https://${formatHostForUrl(host)}:$port/pair")
                // Include a readable Android device name for the Mac pairing UI.
                val request = PairingRequest(
                    code = code,
                    deviceName = DeviceNameResolver.get(context),
                    deviceId = AndroidDeviceIdentity.get(context)
                )

                // Serialize the pairing request into JSON before transmission.
                val json = Gson().toJson(request)

                // Open a direct HTTP connection to the local Mac pairing server.
                val connection = openPinnedConnection(
                    url = url,
                    expectedFingerprint = fingerprint
                )
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

private fun openPinnedConnection(
    url: URL,
    expectedFingerprint: String
): HttpsURLConnection {
    val normalizedExpectedFingerprint = normalizeFingerprint(expectedFingerprint)
    require(normalizedExpectedFingerprint.matches(Regex("[0-9a-f]{64}"))) {
        "Expected TLS fingerprint must be a SHA-256 fingerprint"
    }

    val trustManager = object : X509TrustManager {
        override fun checkClientTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?
        ) = Unit

        override fun checkServerTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?
        ) {
            val certificate = chain?.firstOrNull()
                ?: error("Server certificate is missing")

            if (!constantTimeEquals(
                    certificate.sha256Fingerprint(),
                    normalizedExpectedFingerprint
                )
            ) {
                error("TLS fingerprint mismatch")
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, arrayOf(trustManager), null)

    return (url.openConnection() as HttpsURLConnection).apply {
        sslSocketFactory = sslContext.socketFactory
        hostnameVerifier = PinnedHostnameVerifier(
            expectedHost = url.host,
            expectedFingerprint = normalizedExpectedFingerprint
        )
    }
}
