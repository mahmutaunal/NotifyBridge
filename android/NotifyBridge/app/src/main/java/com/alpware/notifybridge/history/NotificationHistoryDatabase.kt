package com.alpware.notifybridge.history

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class NotificationHistoryDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE notification_history (
                history_id TEXT PRIMARY KEY,
                source_key TEXT NOT NULL UNIQUE,
                device_id TEXT NOT NULL,
                package_name TEXT NOT NULL,
                app_name TEXT,
                title TEXT,
                text TEXT,
                posted_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                removed_at INTEGER,
                lifecycle_state TEXT NOT NULL,
                delivery_state TEXT NOT NULL,
                delivery_attempt_count INTEGER NOT NULL DEFAULT 0,
                delivered_at INTEGER,
                content_hidden INTEGER NOT NULL DEFAULT 0,
                can_dismiss INTEGER NOT NULL DEFAULT 1,
                can_open_on_phone INTEGER NOT NULL DEFAULT 1,
                can_reply INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_history_posted_at ON notification_history(posted_at DESC)")
        db.execSQL("CREATE INDEX idx_history_package ON notification_history(package_name)")
        db.execSQL("CREATE INDEX idx_history_delivery ON notification_history(delivery_state)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS notification_history")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "notifybridge_history.db"
        private const val DATABASE_VERSION = 1
    }
}
