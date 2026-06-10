package com.alpware.notifybridge.pairing


/**
 * Contains the local connection details shared by the Mac app during QR pairing.
 */
data class PairingPayload(
    val host: String,
    val port: Int,
    val code: String,
    val name: String?,
    val fingerprint: String
)