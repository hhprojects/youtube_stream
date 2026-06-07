package com.youtubestream.app.data.remote

import com.youtubestream.app.data.remote.dto.DiscoveryShelfDto
import com.youtubestream.app.data.remote.dto.GenreChartsResponseDto
import com.youtubestream.app.data.remote.dto.MoodDetailDto
import com.youtubestream.app.data.remote.dto.MoodsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/** Discovery namespace on the Pi. Base scheme/host/port come from the BaseUrlInterceptor. */
interface DiscoveryApi {
    @GET("api/discovery/trending")
    suspend fun trending(@Query("region") region: String): DiscoveryShelfDto

    @GET("api/discovery/related")
    suspend fun related(@Query("videoId") videoId: String): DiscoveryShelfDto

    @GET("api/discovery/moods")
    suspend fun moods(): MoodsResponseDto

    @GET("api/discovery/mood")
    suspend fun mood(@Query("params") params: String): MoodDetailDto

    @GET("api/discovery/genre-charts")
    suspend fun genreCharts(@Query("region") region: String): GenreChartsResponseDto

    @GET("api/discovery/playlist")
    suspend fun playlist(@Query("id") id: String): MoodDetailDto
}
