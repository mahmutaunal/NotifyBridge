package com.alpware.notifybridge.network

import android.annotation.SuppressLint
import android.content.Context
import com.alpware.notifybridge.core.PairedMacStore
import com.alpware.notifybridge.model.PairedMac
import java.net.URL
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/** Creates HTTPS connections secured with a per-device certificate fingerprint pin. */
object PinnedHttpsClient {
    fun openConnection(
        context: Context,
        path: String,
        connectTimeout: Int = 3000,
        readTimeout: Int = 3000
    ): HttpsURLConnection = openConnection(
        mac = requireNotNull(PairedMacStore.getSelected(context)) { "No paired Mac" },
        path = path,
        connectTimeout = connectTimeout,
        readTimeout = readTimeout
    )

    fun openConnection(
        mac: PairedMac,
        path: String,
        connectTimeout: Int = 3000,
        readTimeout: Int = 3000
    ): HttpsURLConnection {
        require(mac.host.isNotBlank()) { "Mac IP is empty" }
        require(mac.fingerprint.isNotBlank()) { "Mac TLS fingerprint is empty" }
        val expectedFingerprint = mac.fingerprint.lowercase()

        val trustManager = @SuppressLint("CustomX509TrustManager") object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val certificate = chain?.firstOrNull() ?: error("Server certificate is missing")
                if (certificate.sha256Fingerprint() != expectedFingerprint) error("TLS fingerprint mismatch")
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)
        val url = URL("https://${mac.host}:${mac.port}$path")
        return (url.openConnection() as HttpsURLConnection).apply {
            sslSocketFactory = sslContext.socketFactory
            hostnameVerifier = { _, _ -> true }
            this.connectTimeout = connectTimeout
            this.readTimeout = readTimeout
        }
    }

    private fun X509Certificate.sha256Fingerprint(): String = MessageDigest.getInstance("SHA-256")
        .digest(encoded).joinToString("") { "%02x".format(it) }
}
