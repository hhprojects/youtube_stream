package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastEpisodeItem
import com.youtubestream.app.data.model.PodcastShelf
import com.youtubestream.app.data.model.PodcastShowCard

/** One "latest from your shows" entry: the remote episode + the minimal show info to download it. */
data class LatestEpisode(val showId: String, val showName: String, val episode: PodcastEpisodeItem)

sealed interface PodcastHomeSection {
    /** Downloaded, in-progress episodes — tap plays locally with resume. */
    data class ContinueListening(val episodes: List<PodcastEpisode>) : PodcastHomeSection
    /** Newest episode of each followed show — tap downloads + plays. */
    data class Latest(val items: List<LatestEpisode>) : PodcastHomeSection
    /** A curated/featured shelf of shows — tap opens the show. */
    data class ShowShelf(val label: String, val shows: List<PodcastShowCard>) : PodcastHomeSection
}

/**
 * Pure: order the home as Continue listening → Latest from your shows → show shelves (categories /
 * featured, in the order the backend returned them). Empty shelves are dropped.
 */
fun buildPodcastHome(
    continueListening: List<PodcastEpisode>,
    latest: List<LatestEpisode>,
    showShelves: List<PodcastShelf>,
): List<PodcastHomeSection> {
    val out = mutableListOf<PodcastHomeSection>()
    if (continueListening.isNotEmpty()) out += PodcastHomeSection.ContinueListening(continueListening)
    if (latest.isNotEmpty()) out += PodcastHomeSection.Latest(latest)
    showShelves.forEach { if (it.shows.isNotEmpty()) out += PodcastHomeSection.ShowShelf(it.label, it.shows) }
    return out
}
