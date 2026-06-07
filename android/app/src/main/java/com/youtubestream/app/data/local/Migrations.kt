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

/**
 * v5 → v6: make `filename` the canonical id. Adds the `videoId` column, captures each downloaded
 * row's old id (= its videoId) into it, re-keys the logical-join children (playlist_songs, play_events)
 * from old-id → filename, then flips library_songs.id to the filename. This HEALS rows orphaned by the
 * old id-flip bug. Steps run while id still holds the old value, then flip last. OR IGNORE on the
 * playlist re-key avoids the (playlistId, filename) PK collision from import→download→re-add; the final
 * DELETE purges the skipped duplicates and any pre-existing orphans.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE library_songs ADD COLUMN videoId TEXT")
        db.execSQL("UPDATE library_songs SET videoId = id WHERE id <> filename")
        db.execSQL(
            "UPDATE OR IGNORE playlist_songs SET songId = " +
                "(SELECT filename FROM library_songs WHERE library_songs.id = playlist_songs.songId) " +
                "WHERE songId IN (SELECT id FROM library_songs WHERE id <> filename)",
        )
        db.execSQL(
            "UPDATE play_events SET songId = " +
                "(SELECT filename FROM library_songs WHERE library_songs.id = play_events.songId) " +
                "WHERE songId IN (SELECT id FROM library_songs WHERE id <> filename)",
        )
        db.execSQL("UPDATE library_songs SET id = filename WHERE id <> filename")
        db.execSQL("DELETE FROM playlist_songs WHERE songId NOT IN (SELECT id FROM library_songs)")
    }
}

/**
 * v6 → v7: additive. Creates recent_searches without touching any existing table, so installs keep
 * their library/history/playlists. SQL copied verbatim from schemas/.../7.json (Room-generated).
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recent_searches` " +
                "(`query` TEXT NOT NULL, `usedAt` INTEGER NOT NULL, PRIMARY KEY(`query`))",
        )
    }
}

/**
 * v7 → v8: additive. Creates podcast_episodes + followed_shows without touching any existing table, so
 * installs keep their library/history/playlists/recent-searches. No enforced FK (showId is a logical
 * link — see PodcastEpisode). NOTE: no SQL DEFAULT clauses — a Kotlin field default (resumePositionMs = 0,
 * isFinished = false) is NOT a column default, so Room's expected schema has none either; adding
 * `DEFAULT` here would fail Room's startup identity check. Booleans are INTEGER (0/1).
 * Re-versioned from v6→v7 at merge time: main claimed v7 for recent_searches, so podcasts moved to v7→v8.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `podcast_episodes` " +
                "(`id` TEXT NOT NULL, `videoId` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`showName` TEXT NOT NULL, `showId` TEXT, `durationSeconds` INTEGER NOT NULL, " +
                "`filename` TEXT NOT NULL, `localPath` TEXT NOT NULL, `size` INTEGER NOT NULL, " +
                "`dateAdded` INTEGER NOT NULL, `artworkUrl` TEXT, `publishedDate` TEXT, " +
                "`description` TEXT, `resumePositionMs` INTEGER NOT NULL, " +
                "`isFinished` INTEGER NOT NULL, `lastPlayedAt` INTEGER, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `followed_shows` " +
                "(`showId` TEXT NOT NULL, `title` TEXT NOT NULL, `author` TEXT, " +
                "`artworkUrl` TEXT, `dateFollowed` INTEGER NOT NULL, " +
                "`lastSeenEpisodeVideoId` TEXT, PRIMARY KEY(`showId`))",
        )
    }
}
