package com.alpware.notifybridge.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.alpware.notifybridge.core.MacConnectionStore

/**
 * Discovers compatible NotifyBridge Mac instances on the local network using Bonjour/NSD.
 */
class MacDiscoveryManager(
    private val context: Context,
    private val onResult: (DiscoveryResult) -> Unit
) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null

    /**
     * Starts searching for NotifyBridge services advertised by the Mac app.
     */
    fun startDiscovery() {
        stopDiscovery()

        onResult(DiscoveryResult.Searching)

        val listener = object : NsdManager.DiscoveryListener {

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started: $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")

                // Ignore unrelated Bonjour services on the local network.
                if (serviceInfo.serviceType == SERVICE_TYPE) {
                    resolveService(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                onResult(DiscoveryResult.Error("Discovery not started: $errorCode"))
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                stopDiscovery()
            }
        }

        discoveryListener = listener

        nsdManager.discoverServices(
            SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            listener
        )
    }

    /**
     * Stops the active Bonjour discovery session.
     */
    fun stopDiscovery() {
        discoveryListener?.let {
            runCatching {
                nsdManager.stopServiceDiscovery(it)
            }
        }

        discoveryListener = null
    }

    /**
     * Resolves the discovered Bonjour service into a reachable IP address and port.
     */
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val listener = object : NsdManager.ResolveListener {

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                onResult(DiscoveryResult.Error("Mac çözümlenemedi: $errorCode"))
            }

            override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                val host = resolvedService.host?.hostAddress
                val port = resolvedService.port

                if (host.isNullOrBlank()) {
                    onResult(DiscoveryResult.Error("Mac IP bulunamadı"))
                    return
                }

                MacConnectionStore.setMacIp(context, host)
                MacConnectionStore.setMacPort(context, port.toString())

                onResult(
                    DiscoveryResult.Found(
                        ip = host,
                        port = port.toString(),
                        name = resolvedService.serviceName
                    )
                )

                stopDiscovery()
            }
        }

        resolveListener = listener
        nsdManager.resolveService(serviceInfo, listener)
    }

    companion object {
        private const val TAG = "NotifyBridgeDiscovery"
        private const val SERVICE_TYPE = "_notifybridge._tcp."
    }
}