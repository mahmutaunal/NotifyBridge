package com.alpware.notifybridge.core

import android.content.Context
import com.alpware.notifybridge.model.PairedMac
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/** Secure multi-device persistence with automatic migration from the legacy single-Mac schema. */
object PairedMacStore {
    private const val KEY_ITEMS = "paired_macs_v2"
    private const val KEY_SELECTED = "selected_mac_id_v2"
    private val gson = Gson()

    fun getAll(context: Context): List<PairedMac> {
        migrateLegacyIfNeeded(context)
        val raw = SecureStringStore.getString(context, KEY_ITEMS)
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<PairedMac>>() {}.type
            gson.fromJson<List<PairedMac>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun getEnabled(context: Context): List<PairedMac> = getAll(context).filter { it.enabled && it.isValid }

    fun getSelected(context: Context): PairedMac? {
        val all = getAll(context)
        val selectedId = SecureStringStore.getString(context, KEY_SELECTED)
        return all.firstOrNull { it.id == selectedId } ?: all.firstOrNull()
    }

    fun select(context: Context, id: String) = SecureStringStore.putString(context, KEY_SELECTED, id)

    fun upsert(context: Context, mac: PairedMac, select: Boolean = true) {
        val items = getAll(context).toMutableList()
        val duplicateIndex = items.indexOfFirst { it.id == mac.id || (it.host == mac.host && it.port == mac.port) }
        if (duplicateIndex >= 0) items[duplicateIndex] = mac.copy(id = items[duplicateIndex].id)
        else items.add(mac)
        save(context, items)
        if (select) select(context, items.first { it.host == mac.host && it.port == mac.port }.id)
    }

    fun update(context: Context, id: String, transform: (PairedMac) -> PairedMac) {
        save(context, getAll(context).map { if (it.id == id) transform(it) else it })
    }

    fun remove(context: Context, id: String) {
        val remaining = getAll(context).filterNot { it.id == id }
        save(context, remaining)
        if (SecureStringStore.getString(context, KEY_SELECTED) == id) {
            SecureStringStore.putString(context, KEY_SELECTED, remaining.firstOrNull()?.id.orEmpty())
        }
    }

    private fun save(context: Context, items: List<PairedMac>) {
        SecureStringStore.putString(context, KEY_ITEMS, gson.toJson(items))
    }

    private fun migrateLegacyIfNeeded(context: Context) {
        if (SecureStringStore.getString(context, KEY_ITEMS).isNotBlank()) return
        val host = MacConnectionStore.getMacIp(context)
        val secret = MacConnectionStore.getPairingToken(context)
        val fingerprint = MacConnectionStore.getMacCertFingerprint(context)
        if (host.isBlank() || secret.isBlank() || fingerprint.isBlank()) return
        val legacy = PairedMac(
            id = UUID.randomUUID().toString(),
            name = MacConnectionStore.getMacName(context),
            host = host,
            port = MacConnectionStore.getMacPort(context).toIntOrNull() ?: 8787,
            secret = secret,
            fingerprint = fingerprint
        )
        save(context, listOf(legacy))
        select(context, legacy.id)
    }
}
