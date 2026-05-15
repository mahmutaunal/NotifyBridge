package com.alpware.notifybridge.network

/**
 * Represents the current state of a notification send operation.
 */
sealed class SendResult {

    /**
     * A notification request is currently being sent to the paired Mac.
     */
    data object Loading : SendResult()

    /**
     * The notification was successfully delivered to the Mac app.
     */
    data object Success : SendResult()

    /**
     * The notification request failed due to a network or pairing issue.
     */
    data class Error(
        val message: String
    ) : SendResult()
}