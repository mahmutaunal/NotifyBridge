package com.alpware.notifybridge.network

import android.content.Context
import android.util.Base64
import com.alpware.notifybridge.R
import android.util.Log
import com.alpware.notifybridge.core.AndroidDeviceIdentity
import com.alpware.notifybridge.core.PairedMacStore
import com.alpware.notifybridge.model.PairedMac
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
    private const val TAG = "NotifyBridgeSender"

    /** Fans a notification out to every enabled paired Mac independently. */
    fun send(context: Context, payload: NotificationPayload) {
        val targets = PairedMacStore.getEnabled(context)
        if (targets.isEmpty()) {
            Log.d(TAG, "No enabled paired Mac. Notification not sent.")
            return
        }
        targets.forEach { mac ->
            Thread { runCatching { sendPayload(context, mac, payload) }
                .onFailure { Log.e(TAG, "Send to ${mac.displayName} failed: ${it.message}", it) } }.start()
        }
    }

    fun sendTest(context: Context, onResult: (SendResult) -> Unit) {
        val selected = PairedMacStore.getSelected(context)
        if (selected == null) {
            onResult(SendResult.Error("No paired Mac"))
            return
        }
        onResult(SendResult.Loading)
        val payload = NotificationPayload(
            packageName = "com.alpware.notifybridge",
            appName = context.getString(R.string.app_name),
            title = context.getString(R.string.notification_test_title),
            text = context.getString(R.string.notification_test_message),
            postTime = System.currentTimeMillis(),
            contentHidden = false
        )
        Thread {
            runCatching { sendPayload(context, selected, payload) }
                .onSuccess { onResult(SendResult.Success) }
                .onFailure { onResult(SendResult.Error(it.message ?: "Connection failed")) }
        }.start()
    }

    private fun sendPayload(context: Context, mac: PairedMac, payload: NotificationPayload) {
        val plainJson = Gson().toJson(payload)
        val encryptedPayload = CryptoManager.encryptAesGcm(mac.secret, plainJson)
        val json = Gson().toJson(encryptedPayload)
        val timestamp = System.currentTimeMillis().toString()
        val nonce = UUID.randomUUID().toString()
        val signature = hmacSha256Base64(mac.secret, "$timestamp\n$nonce\n$json")
        val connection = PinnedHttpsClient.openConnection(mac, "/notify")
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("X-NotifyBridge-Device-Id", AndroidDeviceIdentity.get(context))
        connection.setRequestProperty("X-NotifyBridge-Timestamp", timestamp)
        connection.setRequestProperty("X-NotifyBridge-Nonce", nonce)
        connection.setRequestProperty("X-NotifyBridge-Signature", signature)
        OutputStreamWriter(connection.outputStream).use { it.write(json) }
        val code = connection.responseCode
        connection.disconnect()
        check(code == 200) { "HTTP $code" }
    }

    private fun hmacSha256Base64(secret: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return Base64.encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
}
