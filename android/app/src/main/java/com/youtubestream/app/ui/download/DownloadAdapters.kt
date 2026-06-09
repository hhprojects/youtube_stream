package com.youtubestream.app.ui.download

import com.youtubestream.app.data.model.DownloadState
import com.youtubestream.app.data.repository.PodcastDownloadState
import com.youtubestream.app.ui.search.ItemDownload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

/** Adapt a song download's states to the queue's [ItemDownload]: Completed emits nothing (so the queue
 *  removes the key on the flow completing), Failed is kept so the row can offer Retry. */
@JvmName("songStateToItemDownload")
fun Flow<DownloadState>.toItemDownload(): Flow<ItemDownload> = transform { st ->
    when (st) {
        is DownloadState.InProgress -> emit(ItemDownload.Downloading(st.fraction))
        is DownloadState.Completed -> {}
        is DownloadState.Failed -> emit(ItemDownload.Failed(st.error.message ?: "Download failed"))
    }
}

/** Same for an episode download. Completed emits nothing — download is decoupled from playback. */
@JvmName("episodeStateToItemDownload")
fun Flow<PodcastDownloadState>.toItemDownload(): Flow<ItemDownload> = transform { st ->
    when (st) {
        is PodcastDownloadState.InProgress -> emit(ItemDownload.Downloading(st.fraction))
        is PodcastDownloadState.Completed -> {}
        is PodcastDownloadState.Failed -> emit(ItemDownload.Failed(st.error.message ?: "Download failed"))
    }
}
