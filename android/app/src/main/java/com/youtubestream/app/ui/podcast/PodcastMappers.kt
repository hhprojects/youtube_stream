package com.youtubestream.app.ui.podcast

import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastEpisodeItem
import com.youtubestream.app.playback.PlayableTrack
import java.io.File

/** UI row for one episode on the show-detail screen — remote metadata + local download/resume state. */
data class EpisodeRowUi(
    val videoId: String,
    val title: String,
    val durationSeconds: Int,
    val artworkUrl: String?,
    val downloaded: Boolean,
    val localId: String?,            // PodcastEpisode.id (filename) once downloaded; null otherwise
    val resumePositionMs: Long,
    val finished: Boolean,
)

/** Pure: fold the locally-downloaded episodes (keyed by videoId) into the remote, newest-first list. */
fun mergeEpisodeRows(remote: List<PodcastEpisodeItem>, downloaded: List<PodcastEpisode>): List<EpisodeRowUi> {
    val byVideoId = downloaded.associateBy { it.videoId }
    return remote.map { r ->
        val local = byVideoId[r.videoId]
        EpisodeRowUi(
            videoId = r.videoId,
            title = r.title,
            durationSeconds = r.durationSeconds,
            artworkUrl = r.artworkUrl,
            downloaded = local != null,
            localId = local?.id,
            resumePositionMs = local?.resumePositionMs ?: 0L,
            finished = local?.isFinished ?: false,
        )
    }
}

/** Where the listener stands on an episode: nothing started, partway through, or finished. */
enum class EpisodePlayState { UNPLAYED, IN_PROGRESS, PLAYED }

/** Pure: derive play-state from local download/resume info (a not-downloaded episode is always UNPLAYED). */
fun episodePlayState(row: EpisodeRowUi): EpisodePlayState = when {
    row.finished -> EpisodePlayState.PLAYED
    row.downloaded && row.resumePositionMs > 0 -> EpisodePlayState.IN_PROGRESS
    else -> EpisodePlayState.UNPLAYED
}

/** Episode-list view order. Rows arrive newest-first (remote feed order); OLDEST just reverses. */
enum class EpisodeSort { NEWEST, OLDEST }

/** Pure: apply the show-detail view options — optional unplayed-only filter, then sort. */
fun applyEpisodeView(rows: List<EpisodeRowUi>, sort: EpisodeSort, unplayedOnly: Boolean): List<EpisodeRowUi> {
    val filtered = if (unplayedOnly) rows.filter { episodePlayState(it) != EpisodePlayState.PLAYED } else rows
    return if (sort == EpisodeSort.OLDEST) filtered.reversed() else filtered
}

/** Pure: a human duration like "1 hr 49 min" / "45 min" / "1 hr"; empty when unknown (≤0). */
fun formatEpisodeDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h hr"
        else -> "$h hr $m min"
    }
}

/** Pure: the row subtitle — play-state + duration ("Played · 45 min", "35 min left", "45 min"). */
fun episodeSubtitle(row: EpisodeRowUi, state: EpisodePlayState = episodePlayState(row)): String {
    val dur = formatEpisodeDuration(row.durationSeconds)
    return when (state) {
        EpisodePlayState.PLAYED -> if (dur.isEmpty()) "Played" else "Played · $dur"
        EpisodePlayState.IN_PROGRESS -> {
            val leftSec = ((row.durationSeconds * 1000L - row.resumePositionMs) / 1000).coerceAtLeast(0).toInt()
            val left = formatEpisodeDuration(leftSec)
            if (left.isEmpty()) "In progress" else "$left left"
        }
        EpisodePlayState.UNPLAYED -> dur
    }
}

/** Build a player track from a downloaded episode (artist slot = show name). */
fun PodcastEpisode.toPlayableTrack(): PlayableTrack = PlayableTrack(
    mediaId = id,
    uri = File(localPath).toURI().toString(),
    title = title,
    artist = showName,
    artworkUri = artworkUrl,
    isPodcast = true,
)

/** A "Play all" queue (startIndex is always 0 — the queue is exactly the episodes to play). */
data class PlayAllQueue(val episodes: List<PodcastEpisode>, val startPositionMs: Long)

/**
 * Oldest-first downloaded queue for one show. Returns null if the show has no downloaded episodes.
 * Orders by the remote list reversed (authoritative chronological order — publishedDate is unreliable),
 * keeps only downloaded episodes, skips finished, and resumes the oldest unfinished one. If every
 * downloaded episode is finished, replays them all oldest-first from 0 (so the button is never a dead end).
 */
fun playAllQueue(remoteVideoIdsNewestFirst: List<String>, downloaded: List<PodcastEpisode>): PlayAllQueue? {
    val byId = downloaded.associateBy { it.videoId }
    val oldestFirst = remoteVideoIdsNewestFirst.reversed().mapNotNull { byId[it] }
    if (oldestFirst.isEmpty()) return null
    val unfinished = oldestFirst.filter { !it.isFinished }
    return if (unfinished.isNotEmpty()) PlayAllQueue(unfinished, unfinished.first().resumePositionMs)
    else PlayAllQueue(oldestFirst, 0L)
}
