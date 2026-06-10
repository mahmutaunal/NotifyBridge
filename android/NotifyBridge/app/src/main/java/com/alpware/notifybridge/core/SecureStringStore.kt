package com.alpware.notifybridge.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores sensitive string values using Android Keystore-backed AES encryption.
 */
object SecureStringStore {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "notifybridge_secure_store_key"
    private const val PREF_NAME = "notify_bridge_secure_prefs"
    private const val GCM_TAG_LENGTH = 128

    /**
     * Returns a previously stored and decrypted string value.
     */
    fun getString(context: Context, key: String): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val encryptedValue = prefs.getString(key, "") ?: ""

        if (encryptedValue.isBlank()) return ""

        return runCatching {
            decrypt(encryptedValue)
        }.getOrDefault("")
    }

    /**
     * Encrypts and stores a string value.
     */
    fun putString(context: Context, key: String, value: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (value.isBlank()) {
            prefs.edit {
                remove(key)
            }
            return
        }

        prefs.edit {
            putString(key, encrypt(value))
        }
    }

    private fun encrypt(value: String): String {
        // Generate a fresh IV for every encryption operation.
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    /**
     * Decrypts a previously stored encrypted value.
     */
    private fun decrypt(value: String): String {
        // Extract the IV and encrypted payload from the stored value.
        val raw = Base64.decode(value, Base64.NO_WRAP)
        val iv = raw.copyOfRange(0, 12)
        val encrypted = raw.copyOfRange(12, raw.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH, iv)
        )

        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    /**
     * Returns the existing Keystore key or creates a new one when needed.
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }

        // Reuse the existing key to preserve access to previously encrypted values.
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        // Create a new AES key backed by Android Keystore.
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)

        return keyGenerator.generateKey()
    }
}