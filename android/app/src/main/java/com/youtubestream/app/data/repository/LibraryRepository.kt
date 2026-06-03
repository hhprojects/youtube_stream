package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.LibraryDao
import com.youtubestream.app.data.local.LibrarySong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class LibraryRepository(
    private val dao: LibraryDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,   // injectable so tests can run the file IO on virtual time
) {

    fun observeLibrary(): Flow<List<LibrarySong>> = dao.observeAll()

    /** Local-only delete: removes the Room row and the downloaded file. The Pi copy stays for re-import. */
    suspend fun delete(song: LibrarySong) {
        withContext(io) { File(song.localPath).delete() }   // blocking IO off the caller's thread
        dao.deleteById(song.id)
    }

    /** Removes just the Room row by id — used when playback hits a missing local file (file already gone). */
    suspend fun deleteById(id: String) = dao.deleteById(id)
}
