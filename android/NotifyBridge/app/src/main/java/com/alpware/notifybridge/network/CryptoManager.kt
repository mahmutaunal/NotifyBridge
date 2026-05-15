package com.alpware.notifybridge.network

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Handles AES-GCM encryption used for secure local notification transfer.
 */
object CryptoManager {

    /**
     * Encrypts a notification payload before sending it to the paired Mac device.
     */
    fun encryptAesGcm(
        secret: String,
        plainText: String
    ): EncryptedPayload {
        val key = deriveKey(secret)

        // AES-GCM uses a 12-byte IV for optimal performance and security.
        val iv = Random.nextBytes(12)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return EncryptedPayload(
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        )
    }

    /**
     * Derives a stable 256-bit AES key from the pairing secret.
     */
    private fun deriveKey(secret: String): ByteArray {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(secret.toByteArray(Charsets.UTF_8))
    }
}