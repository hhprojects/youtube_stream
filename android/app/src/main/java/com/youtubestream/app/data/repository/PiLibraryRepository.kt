package com.youtubestream.app.data.repository

import com.youtubestream.app.data.model.PiSong
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.LibrarySongDto

class PiLibraryRepository(private val api: YoutubeStreamApi) {
    suspend fun piLibrary(): List<PiSong> = api.library().songs.map { it.toDomain() }

    /** Deletes the file from the Pi by filename. Throws on transport failure or a `success:false` body. */
    suspend fun delete(filename: String) {
        val resp = api.deleteFromPi(filename)
        if (!resp.success) error("The Pi reported the delete failed")
    }
}

private fun LibrarySongDto.toDomain() = PiSong(
    id = id,
    title = title,
    artist = artist,
    filename = filename,
    downloadUrl = downloadUrl,
    size = size,
)   // backend 'duration' ('Unknown') and dateAdded are dropped — not used by the Import screen
