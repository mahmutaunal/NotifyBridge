package com.alpware.notifybridge.pairing

import com.google.gson.annotations.SerializedName


/**
 * Response returned by the Mac app after a successful pairing request.
 */

data class PairingResponse(
    @SerializedName("type")
    val type: String,

    @SerializedName("host")
    val host: String,

    @SerializedName("port")
    val port: Int,

    @SerializedName("secret")
    val secret: String,

    @SerializedName("name")
    val name: String
)