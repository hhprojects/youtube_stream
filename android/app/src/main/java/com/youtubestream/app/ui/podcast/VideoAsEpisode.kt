package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.model.PodcastEpisodeItem
import com.youtubestream.app.data.model.PodcastShowDetail
import com.youtubestream.app.data.model.SearchResult

/** Synthetic show-id prefix for videos downloaded as episodes. Never navigated to — episode rows
 *  don't open show detail — and no real ytmusicapi browseId contains a colon. */
const val VIDEO_SHOW_ID_PREFIX = "video:"

/**
 * A plain YouTube video downloaded as a podcast episode: wrapped in a synthetic show named after
 * the channel, so it lands in Downloaded episodes grouped like any other episode.
 */
fun videoAsEpisode(video: SearchResult): Pair<PodcastShowDetail, PodcastEpisodeItem> =
    PodcastShowDetail(
        showId = VIDEO_SHOW_ID_PREFIX + video.id,
        title = video.channel,
        author = video.channel,
        description = null,
        artworkUrl = video.thumbnailUrl,
        episodes = emptyList(),
    ) to PodcastEpisodeItem(
        videoId = video.id,
        title = video.title,
        durationSeconds = video.durationSeconds ?: 0,
        publishedDate = null,
        description = null,
        artworkUrl = video.thumbnailUrl,
    )
