package com.alpware.notifybridge.pairing

import com.google.gson.annotations.SerializedName


/**
 * Payload sent from Android to the Mac app during the pairing handshake.
 */
data class PairingRequest(
    @SerializedName("code")
    val code: String,

    @SerializedName("deviceName")
    val deviceName: String,

    @SerializedName("deviceId")
    val deviceId: String
)