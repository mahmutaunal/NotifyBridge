package com.alpware.notifybridge.network


/**
 * Represents the current connectivity state between Android and the paired Mac device.
 */
sealed interface ConnectionHealthResult {
    data object Online : ConnectionHealthResult
    data object Offline : ConnectionHealthResult
    data object PairingInvalid : ConnectionHealthResult
}