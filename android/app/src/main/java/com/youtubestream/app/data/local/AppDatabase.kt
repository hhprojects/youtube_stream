package com.youtubestream.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LibrarySong::class, PlayEvent::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun playEventDao(): PlayEventDao
}
