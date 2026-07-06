package com.youtubestream.app.data.remote

import com.youtubestream.app.data.remote.dto.ArtworkRequestDto
import com.youtubestream.app.data.remote.dto.ArtworkResponseDto
import com.youtubestream.app.data.remote.dto.DeleteResponseDto
import com.youtubestream.app.data.remote.dto.DownloadRequestDto
import com.youtubestream.app.data.remote.dto.DownloadResponseDto
import com.youtubestream.app.data.remote.dto.LibraryResponseDto
import com.youtubestream.app.data.remote.dto.SearchRequestDto
import com.youtubestream.app.data.remote.dto.SearchResponseDto
import com.youtubestream.app.data.remote.dto.TitleRequestDto
import com.youtubestream.app.data.remote.dto.TitleResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Maps the Pi backend 1:1. Base scheme/host/port comes from the BaseUrlInterceptor. */
interface YoutubeStreamApi {
    @POST("api/search")
    suspend fun search(@Body body: SearchRequestDto): SearchResponseDto

    @POST("api/download")
    suspend fun download(@Body body: DownloadRequestDto): DownloadResponseDto

    @GET("api/library")
    suspend fun library(): LibraryResponseDto

    /** Deletes the file from the Pi. Retrofit encodes the path segment; non-2xx → HttpException. */
    @DELETE("api/library/{filename}")
    suspend fun deleteFromPi(@Path("filename") filename: String): DeleteResponseDto

    /** Sets the artwork (by videoId) for a Pi file; non-2xx → HttpException. */
    @POST("api/library/{filename}/artwork")
    suspend fun updateArtwork(
        @Path("filename") filename: String,
        @Body body: ArtworkRequestDto,
    ): ArtworkResponseDto

    /** Persists an edited display title into the Pi's metadata sidecar; non-2xx → HttpException. */
    @POST("api/library/{filename}/title")
    suspend fun updateTitle(
        @Path("filename") filename: String,
        @Body body: TitleRequestDto,
    ): TitleResponseDto
}
