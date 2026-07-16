package com.alpware.notifybridge.history

import android.content.ContentValues
import android.content.Context
import com.alpware.notifybridge.notification.NotificationPayload
import java.util.UUID

class NotificationHistoryRepository private constructor(context: Context) {
    private val database = NotificationHistoryDatabase(context)

    fun upsert(payload: NotificationPayload): NotificationPayload {
        val db = database.writableDatabase
        val existingId = db.rawQuery(
            "SELECT history_id FROM notification_history WHERE source_key = ? LIMIT 1",
            arrayOf(payload.notificationKey ?: payload.historyId)
        ).use { if (it.moveToFirst()) it.getString(0) else null }
        val historyId = existingId ?: payload.historyId.ifBlank { UUID.randomUUID().toString() }
        val sourceKey = payload.notificationKey ?: historyId
        val values = ContentValues().apply {
            put("history_id", historyId); put("source_key", sourceKey); put(
            "device_id",
            payload.deviceId.orEmpty()
        )
            put("package_name", payload.packageName); put("app_name", payload.appName); put(
            "title",
            payload.title
        ); put("text", payload.text)
            put("posted_at", payload.postTime); put(
            "updated_at",
            System.currentTimeMillis()
        ); putNull("removed_at")
            put("lifecycle_state", "ACTIVE"); put(
            "delivery_state",
            "PENDING"
        ); put("content_hidden", if (payload.contentHidden) 1 else 0)
            put("can_dismiss", if (payload.canDismiss) 1 else 0); put(
            "can_open_on_phone",
            if (payload.canOpenOnPhone) 1 else 0
        ); put("can_reply", if (payload.canReply) 1 else 0)
        }
        db.insertWithOnConflict(
            "notification_history",
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        cleanup()
        return payload.copy(
            historyId = historyId,
            deviceId = payload.deviceId,
            protocolVersion = 2,
            eventType = "notification_upsert"
        )
    }

    fun markRemoved(sourceKey: String, removedAt: Long = System.currentTimeMillis()) {
        val values = ContentValues().apply {
            put("removed_at", removedAt); put("updated_at", removedAt); put(
            "lifecycle_state",
            "REMOVED"
        )
        }
        database.writableDatabase.update(
            "notification_history",
            values,
            "source_key = ?",
            arrayOf(sourceKey)
        )
    }

    fun markDeliveryAttempt(historyId: String) {
        database.writableDatabase.execSQL(
            "UPDATE notification_history SET delivery_attempt_count = delivery_attempt_count + 1, delivery_state = 'SENDING' WHERE history_id = ?",
            arrayOf(historyId)
        )
    }

    fun markDelivered(historyId: String) {
        val now = System.currentTimeMillis()
        database.writableDatabase.execSQL(
            "UPDATE notification_history SET delivery_state = 'DELIVERED', delivered_at = ?, updated_at = ? WHERE history_id = ?",
            arrayOf<Any?>(now, now, historyId)
        )
    }

    fun markFailed(historyId: String) {
        database.writableDatabase.execSQL(
            "UPDATE notification_history SET delivery_state = 'FAILED_RETRYABLE', updated_at = ? WHERE history_id = ?",
            arrayOf<Any?>(System.currentTimeMillis(), historyId)
        )
    }

    fun list(limit: Int = 500, query: String = ""): List<NotificationHistoryRecord> {
        val q = query.trim()
        val selection =
            if (q.isEmpty()) "" else "WHERE app_name LIKE ? OR package_name LIKE ? OR title LIKE ? OR text LIKE ?"
        val args = if (q.isEmpty()) emptyArray() else Array(4) { "%$q%" }
        return database.readableDatabase.rawQuery(
            "SELECT * FROM notification_history $selection ORDER BY posted_at DESC LIMIT $limit",
            args
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    NotificationHistoryRecord(
                        c.getString(c.getColumnIndexOrThrow("history_id")),
                        c.getString(c.getColumnIndexOrThrow("source_key")),
                        c.getString(c.getColumnIndexOrThrow("device_id")),
                        c.getString(c.getColumnIndexOrThrow("package_name")),
                        c.getStringOrNull("app_name"),
                        c.getStringOrNull("title"),
                        c.getStringOrNull("text"),
                        c.getLong(c.getColumnIndexOrThrow("posted_at")),
                        c.getLong(c.getColumnIndexOrThrow("updated_at")),
                        c.getLongOrNull("removed_at"),
                        c.getString(c.getColumnIndexOrThrow("lifecycle_state")),
                        c.getString(c.getColumnIndexOrThrow("delivery_state")),
                        c.getInt(c.getColumnIndexOrThrow("delivery_attempt_count")),
                        c.getLongOrNull("delivered_at"),
                        c.getInt(c.getColumnIndexOrThrow("content_hidden")) == 1,
                        c.getInt(c.getColumnIndexOrThrow("can_dismiss")) == 1,
                        c.getInt(c.getColumnIndexOrThrow("can_open_on_phone")) == 1,
                        c.getInt(c.getColumnIndexOrThrow("can_reply")) == 1
                    )
                )
            }
        }
    }

    fun clear() = database.writableDatabase.delete("notification_history", null, null)
    fun delete(historyId: String) = database.writableDatabase.delete(
        "notification_history",
        "history_id = ?",
        arrayOf(historyId)
    )

    private fun cleanup() {
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        database.writableDatabase.delete(
            "notification_history",
            "posted_at < ?",
            arrayOf(cutoff.toString())
        )
        database.writableDatabase.execSQL("DELETE FROM notification_history WHERE history_id NOT IN (SELECT history_id FROM notification_history ORDER BY posted_at DESC LIMIT 5000)")
    }

    companion object {
        @Volatile
        private var instance: NotificationHistoryRepository? = null
        fun get(context: Context): NotificationHistoryRepository = instance ?: synchronized(this) {
            instance ?: NotificationHistoryRepository(context.applicationContext).also {
                instance = it
            }
        }
    }
}

private fun android.database.Cursor.getStringOrNull(name: String): String? =
    getColumnIndexOrThrow(name).let { if (isNull(it)) null else getString(it) }

private fun android.database.Cursor.getLongOrNull(name: String): Long? =
    getColumnIndexOrThrow(name).let { if (isNull(it)) null else getLong(it) }
