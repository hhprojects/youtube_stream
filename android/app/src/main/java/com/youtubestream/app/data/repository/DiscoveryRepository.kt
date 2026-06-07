package com.youtubestream.app.data.repository

import com.youtubestream.app.data.model.DiscoverySong
import com.youtubestream.app.data.model.MoodCategory
import com.youtubestream.app.data.model.MoodDetail
import com.youtubestream.app.data.remote.DiscoveryApi
import com.youtubestream.app.data.remote.dto.DiscoverySongDto

/** Testable seam over the discovery endpoints. ViewModels depend on this, not the concrete repo. */
interface DiscoverySource {
    suspend fun trending(region: String): List<DiscoverySong>
    suspend fun related(seedVideoId: String): List<DiscoverySong>
    suspend fun moods(): List<MoodCategory>
    suspend fun moodSongs(key: String): MoodDetail
}

/** Maps discovery DTOs → domain. Throws on IO/HTTP failure; the ViewModel degrades that shelf away. */
class DiscoveryRepository(private val api: DiscoveryApi) : DiscoverySource {
    override suspend fun trending(region: String): List<DiscoverySong> =
        api.trending(region).songs.map { it.toDomain() }

    override suspend fun related(seedVideoId: String): List<DiscoverySong> =
        api.related(seedVideoId).songs.map { it.toDomain() }

    override suspend fun moods(): List<MoodCategory> =
        api.moods().categories.map { MoodCategory(it.key, it.title, it.section) }

    override suspend fun moodSongs(key: String): MoodDetail =
        api.mood(key).let { d -> MoodDetail(d.title, d.songs.map { it.toDomain() }) }
}

private fun DiscoverySongDto.toDomain() = DiscoverySong(
    videoId = id,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnail,
)
