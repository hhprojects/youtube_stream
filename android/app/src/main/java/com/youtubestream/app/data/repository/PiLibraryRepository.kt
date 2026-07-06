package com.youtubestream.app.data.repository

import com.youtubestream.app.data.model.PiSong
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.ArtworkRequestDto
import com.youtubestream.app.data.remote.dto.LibrarySongDto
import com.youtubestream.app.data.remote.dto.TitleRequestDto

class PiLibraryRepository(private val api: YoutubeStreamApi) {
    suspend fun piLibrary(): List<PiSong> = api.library().songs.map { it.toDomain() }

    /** Deletes the file from the Pi by filename. Throws on transport failure or a `success:false` body. */
    suspend fun delete(filename: String) {
        val resp = api.deleteFromPi(filename)
        if (!resp.success) error("The Pi reported the delete failed")
    }

    /** Sets the artwork by videoId on the Pi; returns the Pi-built thumbnail URL. Throws on failure. */
    suspend fun updateArtwork(filename: String, videoId: String): String? {
        val resp = api.updateArtwork(filename, ArtworkRequestDto(videoId))
        if (!resp.success) error("The Pi reported the artwork update failed")
        return resp.thumbnail
    }

    /** Saves an edited title into the Pi's sidecar so re-imports keep it. Throws on failure. */
    suspend fun updateTitle(filename: String, title: String) {
        val resp = api.updateTitle(filename, TitleRequestDto(title))
        if (!resp.success) error("The Pi reported the title update failed")
    }
}

private fun LibrarySongDto.toDomain() = PiSong(
    id = id,
    title = title,
    artist = artist,
    filename = filename,
    downloadUrl = downloadUrl,
    size = size,
    thumbnailUrl = thumbnail,
    videoId = videoId,
)   // backend 'duration' ('Unknown') and dateAdded are dropped — not used by the Import screen
