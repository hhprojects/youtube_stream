package com.youtubestream.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LibrarySong::class, PlayEvent::class, Playlist::class, PlaylistSong::class,
        PodcastEpisode::class, FollowedShow::class, RecentSearch::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun playEventDao(): PlayEventDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun podcastDao(): PodcastDao
    abstract fun recentSearchDao(): RecentSearchDao
}
