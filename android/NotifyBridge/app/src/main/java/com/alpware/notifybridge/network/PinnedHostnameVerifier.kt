package com.alpware.notifybridge.network

import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSession

/**
 * Hostname verifier for NotifyBridge's local-network TLS connections.
 *
 * The Mac uses a per-installation self-signed certificate and may be reached through a changing
 * local IP address. The platform's default hostname verifier therefore cannot be used reliably.
 * This verifier never accepts a host unconditionally: it requires both the exact expected host
 * and the SHA-256 certificate fingerprint captured during pairing.
 */
internal class PinnedHostnameVerifier(
    expectedHost: String,
    expectedFingerprint: String
) : HostnameVerifier {

    private val expectedHost = normalizeHost(expectedHost)
    private val expectedFingerprint = normalizeFingerprint(expectedFingerprint)

    init {
        require(this.expectedHost.isNotBlank()) { "Expected TLS host is empty" }
        require(this.expectedFingerprint.matches(Regex("[0-9a-f]{64}"))) {
            "Expected TLS fingerprint must be a SHA-256 fingerprint"
        }
    }

    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        if (hostname.isNullOrBlank() || session == null) return false
        if (normalizeHost(hostname) != expectedHost) return false

        val certificate = try {
            session.peerCertificates.firstOrNull() as? X509Certificate
        } catch (_: SSLPeerUnverifiedException) {
            null
        } catch (_: RuntimeException) {
            null
        } ?: return false

        return constantTimeEquals(
            certificate.sha256Fingerprint(),
            expectedFingerprint
        )
    }
}

internal fun normalizeFingerprint(value: String): String = value
    .trim()
    .replace(":", "")
    .replace(" ", "")
    .lowercase()

internal fun normalizeHost(value: String): String = value
    .trim()
    .removePrefix("[")
    .removeSuffix("]")
    .removeSuffix(".")
    .lowercase()

internal fun formatHostForUrl(host: String): String {
    val normalized = normalizeHost(host)
    return if (normalized.contains(':')) "[$normalized]" else normalized
}

internal fun X509Certificate.sha256Fingerprint(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(encoded)
    .joinToString(separator = "") { byte -> "%02x".format(byte) }

internal fun constantTimeEquals(first: String, second: String): Boolean = MessageDigest.isEqual(
    first.toByteArray(Charsets.US_ASCII),
    second.toByteArray(Charsets.US_ASCII)
)
