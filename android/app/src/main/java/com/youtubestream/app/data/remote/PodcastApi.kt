package com.youtubestream.app.data.remote

import com.youtubestream.app.data.remote.dto.PodcastDownloadRequestDto
import com.youtubestream.app.data.remote.dto.PodcastDownloadResponseDto
import com.youtubestream.app.data.remote.dto.PodcastHomeDto
import com.youtubestream.app.data.remote.dto.PodcastLatestResponseDto
import com.youtubestream.app.data.remote.dto.PodcastShowDetailDto
import com.youtubestream.app.data.remote.dto.ShowIdsDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Podcast namespace on the Pi. Base scheme/host/port come from the BaseUrlInterceptor. */
interface PodcastApi {
    @GET("api/podcasts/home")
    suspend fun home(): PodcastHomeDto

    @GET("api/podcasts/show/{showId}")
    suspend fun show(@Path("showId") showId: String): PodcastShowDetailDto

    @POST("api/podcasts/download")
    suspend fun download(@Body body: PodcastDownloadRequestDto): PodcastDownloadResponseDto

    @POST("api/podcasts/shows/latest")
    suspend fun latestForShows(@Body body: ShowIdsDto): PodcastLatestResponseDto
}
