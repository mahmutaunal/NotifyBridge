package com.alpware.notifybridge.pairing


/**
 * Response returned by the Mac app after a successful pairing request.
 */
data class PairingResponse(
    val type: String,
    val host: String,
    val port: Int,
    val secret: String,
    val name: String
)