package com.alpware.notifybridge.pairing


/**
 * Payload sent from Android to the Mac app during the pairing handshake.
 */
data class PairingRequest(
    val code: String,
    val deviceName: String
)