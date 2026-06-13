package com.youtubestream.app.data.remote

import com.youtubestream.app.data.remote.dto.LyricsDto
import retrofit2.http.GET
import retrofit2.http.Query

/** Lyrics endpoint on the Pi. Base scheme/host/port come from the BaseUrlInterceptor. */
interface LyricsApi {
    @GET("api/lyrics")
    suspend fun lyrics(
        @Query("filename") filename: String,
        @Query("title") title: String,
        @Query("artist") artist: String,
        @Query("durationSec") durationSec: Int,
    ): LyricsDto
}
