package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.local.FollowedShow
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastEpisodeItem
import com.youtubestream.app.data.model.PodcastFreshShelf
import com.youtubestream.app.data.model.PodcastShelf
import com.youtubestream.app.data.model.PodcastShowCard

/** One "latest from your shows" / "fresh" entry: the remote episode + the minimal show info to download it. */
data class LatestEpisode(val showId: String, val showName: String, val episode: PodcastEpisodeItem)

/** Which view the Podcast home is showing. */
enum class PodcastHomeMode { Popular, Newest }

sealed interface PodcastHomeSection {
    /** The shows the user follows — tap opens the show. (Pinned chrome.) */
    data class Following(val shows: List<FollowedShow>) : PodcastHomeSection
    /** Downloaded, in-progress episodes — tap plays locally with resume. (Pinned chrome.) */
    data class ContinueListening(val episodes: List<PodcastEpisode>) : PodcastHomeSection
    /** The Popular | Newest segmented control. */
    data class ModeToggle(val mode: PodcastHomeMode) : PodcastHomeSection
    /** Newest: newest episode of each followed show — tap downloads. */
    data class Latest(val items: List<LatestEpisode>) : PodcastHomeSection
    /** Newest: per-topic latest episodes (each show's newest) — tap downloads. */
    data class FreshTopic(val label: String, val items: List<LatestEpisode>) : PodcastHomeSection
    /** Popular: a topic shelf of shows — tap opens the show. */
    data class ShowShelf(val label: String, val shows: List<PodcastShowCard>) : PodcastHomeSection
}

/**
 * Pure: assemble the Podcast home for the current mode.
 * Always: pinned Continue-listening → Your-shows (Following) → the mode toggle.
 * Popular → topic show shelves. Newest → Latest-from-your-shows + per-topic Fresh shelves.
 * Empty sections are dropped; the toggle is always present.
 */
fun buildPodcastHome(
    mode: PodcastHomeMode,
    followed: List<FollowedShow>,
    continueListening: List<PodcastEpisode>,
    popularShelves: List<PodcastShelf>,
    latest: List<LatestEpisode>,
    freshTopics: List<PodcastFreshShelf>,
): List<PodcastHomeSection> {
    val out = mutableListOf<PodcastHomeSection>()
    if (continueListening.isNotEmpty()) out += PodcastHomeSection.ContinueListening(continueListening)
    if (followed.isNotEmpty()) out += PodcastHomeSection.Following(followed)
    out += PodcastHomeSection.ModeToggle(mode)
    when (mode) {
        PodcastHomeMode.Popular ->
            popularShelves.forEach { if (it.shows.isNotEmpty()) out += PodcastHomeSection.ShowShelf(it.label, it.shows) }
        PodcastHomeMode.Newest -> {
            if (latest.isNotEmpty()) out += PodcastHomeSection.Latest(latest)
            freshTopics.forEach { if (it.items.isNotEmpty()) out += PodcastHomeSection.FreshTopic(it.label, it.items) }
        }
    }
    return out
}
