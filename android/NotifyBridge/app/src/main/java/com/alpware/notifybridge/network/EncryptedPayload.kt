package com.alpware.notifybridge.network

/**
 * Represents an AES-GCM encrypted payload sent to the paired Mac device.
 */
data class EncryptedPayload(
    val iv: String,
    val ciphertext: String
)