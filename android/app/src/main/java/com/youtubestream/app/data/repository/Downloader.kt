package com.youtubestream.app.data.repository

import com.youtubestream.app.data.model.DownloadState
import kotlinx.coroutines.flow.Flow

/** A narrow seam over [DownloadRepository.download] so ViewModels can be unit-tested with a fake. */
fun interface Downloader {
    fun download(videoId: String, title: String, artworkUrl: String?): Flow<DownloadState>
}
