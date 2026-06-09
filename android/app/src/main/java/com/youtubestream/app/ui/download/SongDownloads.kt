package com.youtubestream.app.ui.download

import com.youtubestream.app.data.model.SearchResult
import com.youtubestream.app.data.repository.Downloader

/** App-scoped song downloads (Search + Discovery), routed through the shared sequential [queue].
 *  Survives leaving the screen because [queue] lives on an app scope. */
class SongDownloads(val queue: DownloadQueue, private val downloader: Downloader) {
    val downloads get() = queue.downloads

    fun enqueue(id: String, title: String, artworkUrl: String?) =
        queue.enqueue(id, title) { downloader.download(id, title, artworkUrl).toItemDownload() }

    fun enqueue(result: SearchResult) = enqueue(result.id, result.title, result.thumbnailUrl)

    fun cancel(id: String) = queue.cancel(id)
}
