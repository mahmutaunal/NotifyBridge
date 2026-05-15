package com.alpware.notifybridge.pairing


/**
 * Contains the local connection details shared by the Mac app during QR pairing.
 */
data class PairingPayload(
    val type: String,
    val host: String,
    val port: Int,
    val secret: String,
    val name: String? = null
)