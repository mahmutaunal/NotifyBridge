package com.alpware.notifybridge.network

/**
 * Represents the current state of automatic Mac discovery on the local network.
 */
sealed class DiscoveryResult {
    /**
     * Bonjour discovery is currently running.
     */
    data object Searching : DiscoveryResult()

    /**
     * A compatible Mac device was discovered on the local network.
     */
    data class Found(
        val ip: String,
        val port: String,
        val name: String
    ) : DiscoveryResult()

    /**
     * Discovery failed due to a network or Bonjour-related issue.
     */
    data class Error(
        val message: String
    ) : DiscoveryResult()
}