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

/**
 * v4 → v5: additive. Creates playlists + playlist_songs without touching library_songs or
 * play_events, so existing installs keep their library and history. No enforced FKs (logical
 * join to library_songs — see PlaylistSong). SQL copied verbatim from schemas/.../5.json.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `coverArtUrl` TEXT, " +
                "`dateCreated` INTEGER NOT NULL, `dateModified` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_songs` " +
                "(`playlistId` INTEGER NOT NULL, `songId` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, `dateAdded` INTEGER NOT NULL, " +
                "PRIMARY KEY(`playlistId`, `songId`))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlist_songs_songId` " +
                "ON `playlist_songs` (`songId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId_position` " +
                "ON `playlist_songs` (`playlistId`, `position`)",
        )
    }
}
