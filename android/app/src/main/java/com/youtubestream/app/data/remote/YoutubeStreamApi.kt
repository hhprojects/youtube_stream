package com.youtubestream.app.data.remote

import com.youtubestream.app.data.remote.dto.DeleteResponseDto
import com.youtubestream.app.data.remote.dto.DownloadRequestDto
import com.youtubestream.app.data.remote.dto.DownloadResponseDto
import com.youtubestream.app.data.remote.dto.LibraryResponseDto
import com.youtubestream.app.data.remote.dto.SearchRequestDto
import com.youtubestream.app.data.remote.dto.SearchResponseDto
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
}
