package com.alpware.notifybridge.network

import android.annotation.SuppressLint
import android.content.Context
import com.alpware.notifybridge.core.PairedMacStore
import com.alpware.notifybridge.model.PairedMac
import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/** Creates HTTPS connections secured with a per-device SHA-256 certificate pin. */
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
        require(mac.port in 1..65535) { "Mac port is invalid" }
        require(path.startsWith('/')) { "Request path must start with /" }

        val expectedFingerprint = normalizeFingerprint(mac.fingerprint)
        require(expectedFingerprint.matches(Regex("[0-9a-f]{64}"))) {
            "Mac TLS fingerprint must be a SHA-256 fingerprint"
        }

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
                val certificate = chain?.firstOrNull()
                    ?: error("Server certificate is missing")

                if (!constantTimeEquals(
                        certificate.sha256Fingerprint(),
                        expectedFingerprint
                    )
                ) {
                    error("TLS fingerprint mismatch")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)

        val url = URL(
            "https://${formatHostForUrl(mac.host)}:${mac.port}$path"
        )

        return (url.openConnection() as HttpsURLConnection).apply {
            sslSocketFactory = sslContext.socketFactory
            hostnameVerifier = PinnedHostnameVerifier(
                expectedHost = mac.host,
                expectedFingerprint = expectedFingerprint
            )
            this.connectTimeout = connectTimeout
            this.readTimeout = readTimeout
        }
    }
}
