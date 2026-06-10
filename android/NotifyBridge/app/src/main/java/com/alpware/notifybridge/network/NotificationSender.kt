package com.alpware.notifybridge.network

import android.content.Context
import android.util.Base64
import com.alpware.notifybridge.R
import android.util.Log
import com.alpware.notifybridge.core.MacConnectionStore
import com.alpware.notifybridge.notification.NotificationPayload
import com.google.gson.Gson
import java.io.OutputStreamWriter
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Sends encrypted notification payloads from Android to the paired Mac device.
 */
/**
 * Utility object responsible for sending encrypted notification payloads to the paired Mac device.
 *
 * Handles encryption, signing, and HTTP POST delivery with replay protection.
 */
object NotificationSender {

    // Shared log tag used for notification delivery operations.
    private const val TAG = "NotifyBridgeSender"

    /**
     * Sends a real Android notification payload to the paired Mac in the background.
     *
     * @param context Android context for accessing preferences and resources.
     * @param payload Notification payload to send.
     */
    fun send(context: Context, payload: NotificationPayload) {
        Thread {
            try {
                val macIp = MacConnectionStore.getMacIp(context)

                if (macIp.isBlank()) {
                    Log.d(TAG, "Mac IP is empty. Notification not sent.")
                    return@Thread
                }

                val pairingToken = MacConnectionStore.getPairingToken(context)
                if (pairingToken.isBlank()) {
                    Log.d(TAG, "Pairing token is empty. Notification not sent.")
                    return@Thread
                }

                // Encrypt the notification payload before sending it over the network.
                val plainJson = Gson().toJson(payload)
                val encryptedPayload = CryptoManager.encryptAesGcm(
                    secret = pairingToken,
                    plainText = plainJson
                )
                val json = Gson().toJson(encryptedPayload)

                val timestamp = System.currentTimeMillis().toString()
                val nonce = UUID.randomUUID().toString()
                // Include timestamp and nonce to prevent replay attacks.
                val signingMessage = "$timestamp\n$nonce\n$json"
                val signature = hmacSha256Base64(pairingToken, signingMessage)

                // Attach signed request metadata for basic request validation.
                val connection = PinnedHttpsClient.openConnection(
                    context = context,
                    path = "/notify"
                )
                connection.requestMethod = "POST"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-NotifyBridge-Timestamp", timestamp)
                connection.setRequestProperty("X-NotifyBridge-Nonce", nonce)
                connection.setRequestProperty("X-NotifyBridge-Signature", signature)

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(json)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "Notification sent. Response code: $responseCode")

                connection.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Send failed: ${e.message}", e)
            }
        }.start()
    }

    /**
     * Sends a test notification and reports the result back to the UI.
     *
     * @param context Android context for accessing preferences and resources.
     * @param onResult Callback to report sending result.
     */
    fun sendTest(
        context: Context,
        onResult: (SendResult) -> Unit
    ) {
        onResult(SendResult.Loading)

        // Creates a local sample payload used to verify Mac connectivity.
        val payload = NotificationPayload(
            packageName = "com.alpware.notifybridge",
            appName = context.getString(R.string.app_name),
            title = context.getString(R.string.notification_test_title),
            text = context.getString(R.string.notification_test_message),
            postTime = System.currentTimeMillis(),
            contentHidden = false
        )

        Thread {
            try {
                val macIp = MacConnectionStore.getMacIp(context)

                if (macIp.isBlank()) {
                    onResult(SendResult.Error("Mac IP empty"))
                    return@Thread
                }

                val pairingToken = MacConnectionStore.getPairingToken(context)
                if (pairingToken.isBlank()) {
                    Log.d(TAG, "Pairing token is empty. Notification not sent.")
                    return@Thread
                }

                // Reuse the same encryption flow used for real notifications.
                val plainJson = Gson().toJson(payload)
                val encryptedPayload = CryptoManager.encryptAesGcm(
                    secret = pairingToken,
                    plainText = plainJson
                )
                val json = Gson().toJson(encryptedPayload)

                val timestamp = System.currentTimeMillis().toString()
                val nonce = UUID.randomUUID().toString()
                val signingMessage = "$timestamp\n$nonce\n$json"
                val signature = hmacSha256Base64(pairingToken, signingMessage)

                // Attach request verification headers before sending the test payload.
                val connection = PinnedHttpsClient.openConnection(
                    context = context,
                    path = "/notify"
                )
                connection.requestMethod = "POST"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-NotifyBridge-Timestamp", timestamp)
                connection.setRequestProperty("X-NotifyBridge-Nonce", nonce)
                connection.setRequestProperty("X-NotifyBridge-Signature", signature)

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(json)
                    writer.flush()
                }

                val responseCode = connection.responseCode

                connection.disconnect()

                if (responseCode == 200) {
                    onResult(SendResult.Success)
                } else {
                    onResult(
                        SendResult.Error(
                            "HTTP hata kodu: $responseCode"
                        )
                    )
                }

            } catch (e: Exception) {
                onResult(
                    SendResult.Error(
                        e.message ?: "Bağlantı başarısız"
                    )
                )
            }
        }.start()
    }

    /**
     * Creates a Base64 encoded HMAC-SHA256 signature for request verification.
     *
     * @param secret Shared secret key for signing.
     * @param message Message to sign.
     * @return Base64-encoded HMAC-SHA256 signature string.
     */
    private fun hmacSha256Base64(secret: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)

        val result = mac.doFinal(message.toByteArray(Charsets.UTF_8))

        return Base64.encodeToString(result, Base64.NO_WRAP)
    }
}