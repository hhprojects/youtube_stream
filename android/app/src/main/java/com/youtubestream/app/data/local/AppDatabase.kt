package com.youtubestream.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LibrarySong::class, PlayEvent::class, Playlist::class, PlaylistSong::class, RecentSearch::class],
    version = 7,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun playEventDao(): PlayEventDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentSearchDao(): RecentSearchDao
}
