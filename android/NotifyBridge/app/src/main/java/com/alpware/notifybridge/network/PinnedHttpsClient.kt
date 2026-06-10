package com.alpware.notifybridge.network

import android.annotation.SuppressLint
import android.content.Context
import com.alpware.notifybridge.core.MacConnectionStore
import java.net.URL
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Creates HTTPS connections secured with certificate fingerprint pinning.
 */
object PinnedHttpsClient {

    /**
     * Opens a pinned HTTPS connection to the paired Mac device.
     */
    fun openConnection(
        context: Context,
        path: String,
        connectTimeout: Int = 3000,
        readTimeout: Int = 3000
    ): HttpsURLConnection {
        // Load the stored pairing and TLS pinning information.
        val host = MacConnectionStore.getMacIp(context)
        val port = MacConnectionStore.getMacPort(context)
        val expectedFingerprint = MacConnectionStore
            .getMacCertFingerprint(context)
            .lowercase()

        require(host.isNotBlank()) { "Mac IP is empty" }
        require(expectedFingerprint.isNotBlank()) { "Mac TLS fingerprint is empty" }

        val trustManager = @SuppressLint("CustomX509TrustManager")
        object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) = Unit

            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
                // Validate the server certificate against the stored fingerprint.
                val certificate = chain?.firstOrNull()
                    ?: error("Server certificate is missing")

                val actualFingerprint = certificate.sha256Fingerprint()

                if (actualFingerprint != expectedFingerprint) {
                    error("TLS fingerprint mismatch")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        // Build a dedicated TLS context using the custom trust manager.
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)

        // Create a pinned HTTPS connection to the requested endpoint.
        val url = URL("https://$host:$port$path")
        return (url.openConnection() as HttpsURLConnection).apply {
            sslSocketFactory = sslContext.socketFactory
            hostnameVerifier = { _, _ -> true }
            this.connectTimeout = connectTimeout
            this.readTimeout = readTimeout
        }
    }

    /**
     * Returns the SHA-256 fingerprint of a certificate as a lowercase hex string.
     */
    private fun X509Certificate.sha256Fingerprint(): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(encoded)

        return digest.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }
}