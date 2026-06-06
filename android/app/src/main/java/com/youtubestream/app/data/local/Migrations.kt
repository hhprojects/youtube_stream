package com.youtubestream.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v3 → v4: additive. Creates the play_events table without touching library_songs,
 * so existing installs keep their library (no forced re-import) and play-history —
 * which re-derives from nowhere — survives this and future bumps.
 * SQL copied verbatim from schemas/.../4.json (Room-generated).
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `play_events` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`songId` TEXT NOT NULL, `playedAt` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_songId` ON `play_events` (`songId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_playedAt` ON `play_events` (`playedAt`)")
    }
}
