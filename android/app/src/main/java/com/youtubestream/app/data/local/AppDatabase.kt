package com.youtubestream.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LibrarySong::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
