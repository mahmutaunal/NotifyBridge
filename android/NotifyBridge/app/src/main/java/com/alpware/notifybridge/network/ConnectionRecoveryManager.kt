package com.alpware.notifybridge.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.alpware.notifybridge.core.PairedMacStore
import com.alpware.notifybridge.model.PairedMac
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Restores a paired connection after either app is restarted or the Mac receives a new LAN IP.
 * A discovered endpoint is accepted only after the existing TLS fingerprint and pairing token pass.
 */
object ConnectionRecoveryManager {
    private const val TAG = "NotifyBridgeRecovery"
    private const val SERVICE_TYPE = "_notifybridge._tcp."
    private const val DISCOVERY_TIMEOUT_MS = 8_000L
    private val recovering = AtomicBoolean(false)
    private val callbacks = CopyOnWriteArrayList<(ConnectionHealthResult) -> Unit>()

    fun recover(context: Context, onResult: (ConnectionHealthResult) -> Unit = {}) {
        callbacks += onResult
        val appContext = context.applicationContext
        val selected = PairedMacStore.getSelected(appContext)
        if (selected == null || !selected.isValid) {
            complete(ConnectionHealthResult.PairingInvalid)
            return
        }
        if (!recovering.compareAndSet(false, true)) return

        Thread {
            val direct = ConnectionHealthClient.checkBlocking(appContext, selected)
            if (direct == ConnectionHealthResult.Online || direct == ConnectionHealthResult.PairingInvalid) {
                complete(direct)
            } else {
                discoverAndRecover(appContext, selected)
            }
        }.start()
    }

    private fun discoverAndRecover(context: Context, selected: PairedMac) {
        val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val mainHandler = Handler(Looper.getMainLooper())
        val finished = AtomicBoolean(false)
        lateinit var listener: NsdManager.DiscoveryListener
        lateinit var timeoutRunnable: Runnable

        fun finish(result: ConnectionHealthResult) {
            if (!finished.compareAndSet(false, true)) return
            mainHandler.removeCallbacks(timeoutRunnable)
            mainHandler.post {
                runCatching { nsd.stopServiceDiscovery(listener) }
                complete(result)
            }
        }

        listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE || finished.get()) return
                runCatching {
                    nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            val host = resolved.host?.hostAddress?.substringBefore('%').orEmpty()
                            if (host.isBlank() || finished.get()) return

                            val candidate = selected.copy(
                                host = host,
                                port = resolved.port,
                                name = resolved.serviceName.ifBlank { selected.name }
                            )
                            Thread {
                                if (ConnectionHealthClient.checkBlocking(context, candidate) == ConnectionHealthResult.Online) {
                                    PairedMacStore.update(context, selected.id) {
                                        candidate.copy(lastSeenAt = System.currentTimeMillis())
                                    }
                                    Log.i(TAG, "Recovered paired Mac at $host:${resolved.port}")
                                    finish(ConnectionHealthResult.Online)
                                }
                            }.start()
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                finish(ConnectionHealthResult.Offline)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }

        timeoutRunnable = Runnable { finish(ConnectionHealthResult.Offline) }
        mainHandler.postDelayed(timeoutRunnable, DISCOVERY_TIMEOUT_MS)
        mainHandler.post {
            runCatching { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
                .onFailure { finish(ConnectionHealthResult.Offline) }
        }
    }

    private fun complete(result: ConnectionHealthResult) {
        recovering.set(false)
        val pending = callbacks.toList()
        callbacks.removeAll(pending.toSet())
        pending.forEach { callback -> runCatching { callback(result) } }
    }
}
