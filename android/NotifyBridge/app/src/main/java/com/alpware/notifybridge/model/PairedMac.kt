package com.alpware.notifybridge.model

data class PairedMac(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val secret: String,
    val fingerprint: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long? = null
) {
    val displayName: String get() = name.ifBlank { host }
    val isValid: Boolean get() = host.isNotBlank() && secret.isNotBlank() && fingerprint.isNotBlank()
}
