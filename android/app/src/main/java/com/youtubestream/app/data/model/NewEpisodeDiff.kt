package com.youtubestream.app.data.model

/** Result of diffing one show's latest episodes against the last-seen anchor. */
data class NewEpisodeDiff(val newVideoIds: List<String>, val newAnchor: String?)

/**
 * Given a show's episode videoIds (newest-first, as get_podcast returns them) and the last-seen
 * anchor, return the episodes above the anchor (the new ones) and the anchor to store next.
 * - anchor found → everything before it is new; advance anchor to the current top.
 * - anchor null (first check) or not found (scrolled past the fetch limit) → report nothing new
 *   and (re)set the anchor to the top, so we never spam on the first sight or after a gap.
 * - empty list → nothing new, anchor unchanged.
 */
fun newEpisodesSince(latestVideoIds: List<String>, lastSeenVideoId: String?): NewEpisodeDiff {
    val top = latestVideoIds.firstOrNull() ?: return NewEpisodeDiff(emptyList(), lastSeenVideoId)
    val idx = latestVideoIds.indexOf(lastSeenVideoId)
    if (lastSeenVideoId == null || idx < 0) return NewEpisodeDiff(emptyList(), top)
    return NewEpisodeDiff(latestVideoIds.subList(0, idx).toList(), top)
}

/** A followed show that has new episodes, for building the notification summary. */
data class ShowNewEpisodes(val showName: String, val count: Int)

/** Summary line for the notification, or null when there's nothing to post. */
fun newEpisodeNotificationText(shows: List<ShowNewEpisodes>): String? = when {
    shows.isEmpty() -> null
    shows.size == 1 -> shows[0].let {
        if (it.count == 1) "New episode from ${it.showName}" else "${it.count} new episodes from ${it.showName}"
    }
    else -> "New episodes from ${shows.size} shows you follow"
}
