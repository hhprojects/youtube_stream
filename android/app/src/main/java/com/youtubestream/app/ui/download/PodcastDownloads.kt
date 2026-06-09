package com.youtubestream.app.ui.download

import com.youtubestream.app.data.model.PodcastEpisodeItem
import com.youtubestream.app.data.model.PodcastShowDetail
import com.youtubestream.app.data.repository.PodcastSource

/** App-scoped episode downloads, routed through the shared sequential [queue]. Survives leaving the
 *  screen; does NOT auto-play on completion (download is decoupled from playback). */
class PodcastDownloads(val queue: DownloadQueue, private val repo: PodcastSource) {
    val downloads get() = queue.downloads

    fun enqueue(show: PodcastShowDetail, episode: PodcastEpisodeItem) =
        queue.enqueue(episode.videoId, episode.title) { repo.downloadEpisode(show, episode).toItemDownload() }

    fun cancel(videoId: String) = queue.cancel(videoId)
}
