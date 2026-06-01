package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.LibraryDao
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.model.DownloadState
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.DownloadRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Two-step download: POST /api/download → stream the returned file into [songsDir], emitting
 * progress, then insert the Room row. Cancellation-safe — a partial file is deleted on failure.
 * The returned Flow does blocking IO; collect it on a background dispatcher.
 */
class DownloadRepository(
    private val api: YoutubeStreamApi,
    private val fileClient: OkHttpClient,   // 300s-timeout client for the file stream
    private val dao: LibraryDao,
    private val songsDir: File,             // filesDir/songs
    private val baseUrl: () -> String,      // resolves a relative downloadUrl (real ones are absolute)
) {
    fun download(videoId: String, title: String): Flow<DownloadState> = flow {
        val meta = api.download(DownloadRequestDto(videoId, title))
        val url = if (meta.downloadUrl.startsWith("http")) meta.downloadUrl
        else baseUrl().removeSuffix("/") + meta.downloadUrl
        val target = File(songsDir.apply { mkdirs() }, meta.filename)
        try {
            fileClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val body = resp.body ?: error("empty response body")
                val total = if (body.contentLength() > 0) body.contentLength() else meta.size
                var read = 0L
                target.outputStream().use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            read += n
                            if (total > 0) emit(DownloadState.InProgress(read.toFloat() / total))
                        }
                    }
                }
            }
            val song = LibrarySong(
                id = videoId,
                title = meta.title,
                artist = meta.artist,
                durationSeconds = 0,    // /api/download has no duration; read from the file later
                filename = meta.filename,
                localPath = target.absolutePath,
                size = meta.size,
                dateAdded = System.currentTimeMillis(),
            )
            dao.insert(song)
            emit(DownloadState.Completed(song))
        } catch (t: Throwable) {
            target.delete()             // no orphan partial file
            emit(DownloadState.Failed(t))
        }
    }
}
