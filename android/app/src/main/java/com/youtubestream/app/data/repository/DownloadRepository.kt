package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.LibraryDao
import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.model.DownloadState
import com.youtubestream.app.data.model.PiSong
import com.youtubestream.app.data.remote.YoutubeStreamApi
import com.youtubestream.app.data.remote.dto.DownloadRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** A song that already exists on the Pi → streamed straight into the local library (no re-download). */
fun interface Importer {
    fun importSong(song: PiSong): Flow<DownloadState>
}

/**
 * Streams a file into [songsDir], emitting progress, then inserts the Room row. Cancellation-safe —
 * a partial file is deleted on failure. The returned Flow does blocking IO; collect off the main thread.
 */
class DownloadRepository(
    private val api: YoutubeStreamApi,
    private val fileClient: OkHttpClient,   // 300s-timeout client for the file stream
    private val dao: LibraryDao,
    private val songsDir: File,             // filesDir/songs
    private val baseUrl: () -> String,      // resolves a relative downloadUrl (real ones are absolute)
) : Downloader, Importer {

    /** Two-step: POST /api/download (yt-dlp fetches from YouTube), then stream the new file in. */
    override fun download(videoId: String, title: String): Flow<DownloadState> = flow {
        val meta = api.download(DownloadRequestDto(videoId, title))
        val target = File(songsDir.apply { mkdirs() }, meta.filename)
        try {
            streamTo(absoluteUrl(meta.downloadUrl), target, meta.size)
            val song = LibrarySong(
                id = videoId,
                title = meta.title,
                artist = meta.artist,
                durationSeconds = 0,
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

    /** One-step: the Pi already has the file (live absolute downloadUrl) — stream it straight in. */
    override fun importSong(song: PiSong): Flow<DownloadState> = flow {
        val target = File(songsDir.apply { mkdirs() }, song.filename)
        try {
            streamTo(absoluteUrl(song.downloadUrl), target, song.size)
            val row = LibrarySong(
                id = song.id,
                title = song.title,
                artist = song.artist,
                durationSeconds = 0,
                filename = song.filename,
                localPath = target.absolutePath,
                size = song.size,
                dateAdded = System.currentTimeMillis(),
            )
            dao.insert(row)
            emit(DownloadState.Completed(row))
        } catch (t: Throwable) {
            target.delete()
            emit(DownloadState.Failed(t))
        }
    }

    private fun absoluteUrl(downloadUrl: String): String =
        if (downloadUrl.startsWith("http")) downloadUrl else baseUrl().removeSuffix("/") + downloadUrl

    /**
     * Streams [url] into [target], emitting InProgress; falls back to [fallbackTotal] when the
     * response has no Content-Length. A member extension on the collector so both `download` and
     * `importSong` can `emit(...)` through it.
     */
    private suspend fun FlowCollector<DownloadState>.streamTo(url: String, target: File, fallbackTotal: Long) {
        fileClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            val body = resp.body ?: error("empty response body")
            val total = if (body.contentLength() > 0) body.contentLength() else fallbackTotal
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
    }
}
