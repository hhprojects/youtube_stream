package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.FollowedShow
import com.youtubestream.app.data.local.PodcastDao
import com.youtubestream.app.data.local.PodcastEpisode
import com.youtubestream.app.data.model.PodcastEpisodeItem
import com.youtubestream.app.data.model.PodcastShelf
import com.youtubestream.app.data.model.PodcastShowCard
import com.youtubestream.app.data.model.PodcastShowDetail
import com.youtubestream.app.data.model.ShowNewEpisodes
import com.youtubestream.app.data.model.newEpisodesSince
import com.youtubestream.app.data.model.parsePodcastDuration
import com.youtubestream.app.data.remote.PodcastApi
import com.youtubestream.app.data.remote.dto.PodcastDownloadRequestDto
import com.youtubestream.app.data.remote.dto.ShowIdsDto
import com.youtubestream.app.ui.podcast.LatestEpisode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** Progress for one episode download (parallels DownloadState, but Completed carries a PodcastEpisode). */
sealed interface PodcastDownloadState {
    data class InProgress(val fraction: Float) : PodcastDownloadState
    data class Completed(val episode: PodcastEpisode) : PodcastDownloadState
    data class Failed(val error: Throwable) : PodcastDownloadState
}

interface PodcastSource {
    suspend fun home(): List<PodcastShelf>
    suspend fun show(showId: String): PodcastShowDetail
    fun observeDownloadedEpisodes(): Flow<List<PodcastEpisode>>
    fun observeIsFollowing(showId: String): Flow<Boolean>
    suspend fun follow(detail: PodcastShowDetail)
    suspend fun unfollow(showId: String)
    fun downloadEpisode(show: PodcastShowDetail, episode: PodcastEpisodeItem): Flow<PodcastDownloadState>
    suspend fun updateResumePosition(id: String, positionMs: Long, playedAt: Long)
    suspend fun markFinished(id: String)
    suspend fun isEpisode(mediaId: String): Boolean
    suspend fun getEpisode(id: String): PodcastEpisode?
    fun observeContinueListening(): Flow<List<PodcastEpisode>>
    fun observeFollowedShows(): Flow<List<FollowedShow>>
    suspend fun followedShowIds(): List<String>
    suspend fun latestFromShows(showIds: List<String>): List<LatestEpisode>

    /** Background check: diff each followed show's latest episodes vs its anchor, advance anchors,
     *  and return the shows that gained new episodes (for the notification). Network call inside. */
    suspend fun checkForNewEpisodes(): List<ShowNewEpisodes>

    /** Search YouTube Music podcast shows by name (results carry no author — filled on the detail screen). */
    suspend fun search(query: String): List<PodcastShowCard>
}

/**
 * Mirrors DiscoveryRepository (DTO→domain) + DownloadRepository (download→stream→insert). Episodes
 * insert a PodcastEpisode keyed on filename; podcasts live in their own dir (no auto-prune).
 */
class PodcastRepository(
    private val api: PodcastApi,              // home/show — fast, cached on the Pi
    private val downloadApi: PodcastApi,      // the slow download POST — long-timeout client
    private val dao: PodcastDao,
    private val fileClient: OkHttpClient,     // streams the m4a; BaseUrlInterceptor rewrites host
    private val podcastsDir: File,
    private val baseUrl: () -> String,        // resolves a relative downloadUrl (real ones are absolute)
    private val clock: () -> Long = { System.currentTimeMillis() },
) : PodcastSource {

    override suspend fun home(): List<PodcastShelf> =
        api.home().shelves.map { s ->
            PodcastShelf(s.label, s.shows.map { PodcastShowCard(it.showId, it.title, it.author, it.thumbnail) })
        }

    override suspend fun show(showId: String): PodcastShowDetail =
        api.show(showId).let { d ->
            PodcastShowDetail(
                showId = d.showId,
                title = d.title,
                author = d.author,
                description = d.description,
                artworkUrl = d.thumbnail,
                episodes = d.episodes.map {
                    PodcastEpisodeItem(
                        videoId = it.videoId,
                        title = it.title,
                        durationSeconds = parsePodcastDuration(it.duration),
                        publishedDate = it.date,
                        description = it.description,
                        artworkUrl = it.thumbnail,
                    )
                },
            )
        }

    override fun observeDownloadedEpisodes(): Flow<List<PodcastEpisode>> = dao.observeDownloadedEpisodes()
    override fun observeIsFollowing(showId: String): Flow<Boolean> = dao.observeIsFollowing(showId)

    override suspend fun follow(detail: PodcastShowDetail) =
        dao.follow(
            FollowedShow(
                showId = detail.showId,
                title = detail.title,
                author = detail.author,
                artworkUrl = detail.artworkUrl,
                dateFollowed = clock(),
                lastSeenEpisodeVideoId = detail.episodes.firstOrNull()?.videoId,  // anchor "new" at current top
            ),
        )

    override suspend fun unfollow(showId: String) = dao.unfollow(showId)

    override fun downloadEpisode(show: PodcastShowDetail, episode: PodcastEpisodeItem): Flow<PodcastDownloadState> = flow {
        var target: File? = null
        try {
            val meta = downloadApi.download(
                PodcastDownloadRequestDto(
                    videoId = episode.videoId,
                    title = episode.title,
                    showName = show.title,
                    showId = show.showId,
                    date = episode.publishedDate,
                    description = episode.description,
                    artworkUrl = episode.artworkUrl,
                ),
            )
            if (!meta.success) error("The Pi reported the download failed")
            target = File(podcastsDir.apply { mkdirs() }, meta.filename)
            streamTo(absoluteUrl(meta.downloadUrl), target, meta.size)
            val ep = PodcastEpisode(
                id = meta.filename,                       // canonical id = filename
                videoId = episode.videoId,
                title = episode.title,
                showName = show.title,
                showId = show.showId,
                durationSeconds = episode.durationSeconds,
                filename = meta.filename,
                localPath = target.absolutePath,
                size = meta.size,
                dateAdded = clock(),
                artworkUrl = episode.artworkUrl,
                publishedDate = episode.publishedDate,
                description = episode.description,
            )
            dao.insertEpisode(ep)
            emit(PodcastDownloadState.Completed(ep))
        } catch (c: CancellationException) {
            target?.delete(); throw c
        } catch (t: Throwable) {
            target?.delete(); emit(PodcastDownloadState.Failed(t))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun updateResumePosition(id: String, positionMs: Long, playedAt: Long) =
        dao.updateResumePosition(id, positionMs, playedAt)
    override suspend fun markFinished(id: String) = dao.markFinished(id)
    override suspend fun isEpisode(mediaId: String): Boolean = dao.episodeExists(mediaId)
    override suspend fun getEpisode(id: String): PodcastEpisode? = dao.getEpisode(id)

    override fun observeContinueListening(): Flow<List<PodcastEpisode>> = dao.observeContinueListening()
    override fun observeFollowedShows(): Flow<List<FollowedShow>> = dao.observeFollowedShows()

    override suspend fun followedShowIds(): List<String> =
        dao.observeFollowedShows().first().map { it.showId }

    override suspend fun latestFromShows(showIds: List<String>): List<LatestEpisode> {
        if (showIds.isEmpty()) return emptyList()
        return api.latestForShows(ShowIdsDto(showIds)).shows.mapNotNull { s ->
            val top = s.episodes.firstOrNull() ?: return@mapNotNull null   // newest-first → [0]
            LatestEpisode(
                showId = s.showId,
                showName = s.title ?: "",
                episode = PodcastEpisodeItem(
                    videoId = top.videoId,
                    title = top.title,
                    durationSeconds = parsePodcastDuration(top.duration),
                    publishedDate = top.date,
                    description = top.description,
                    artworkUrl = top.thumbnail,
                ),
            )
        }
    }

    override suspend fun checkForNewEpisodes(): List<ShowNewEpisodes> {
        val followed = dao.observeFollowedShows().first()
        if (followed.isEmpty()) return emptyList()
        val byId = followed.associateBy { it.showId }
        val resp = api.latestForShows(ShowIdsDto(followed.map { it.showId }))
        val out = mutableListOf<ShowNewEpisodes>()
        for (s in resp.shows) {
            val show = byId[s.showId] ?: continue
            val diff = newEpisodesSince(s.episodes.map { it.videoId }, show.lastSeenEpisodeVideoId)
            // Advance the anchor whenever it moved (new episodes, first sight, or a reset).
            if (diff.newAnchor != null && diff.newAnchor != show.lastSeenEpisodeVideoId) {
                dao.updateLastSeenEpisode(s.showId, diff.newAnchor)
            }
            if (diff.newVideoIds.isNotEmpty()) {
                out += ShowNewEpisodes(showName = show.title, count = diff.newVideoIds.size)
            }
        }
        return out
    }

    override suspend fun search(query: String): List<PodcastShowCard> =
        api.search(query).shows.map { PodcastShowCard(it.showId, it.title, it.author, it.thumbnail) }

    // Same idiom as DownloadRepository: absolute URLs pass through; the fileClient's BaseUrlInterceptor
    // rewrites host/port to the configured Pi. Relative URLs are prefixed with the base URL.
    private fun absoluteUrl(downloadUrl: String): String =
        if (downloadUrl.startsWith("http")) downloadUrl else baseUrl().removeSuffix("/") + downloadUrl

    private suspend fun FlowCollector<PodcastDownloadState>.streamTo(url: String, target: File, fallbackTotal: Long) {
        fileClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            val body = resp.body ?: error("empty response body")
            val total = if (body.contentLength() > 0) body.contentLength() else fallbackTotal
            var read = 0L
            target.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        out.write(buf, 0, n); read += n
                        if (total > 0) emit(PodcastDownloadState.InProgress(read.toFloat() / total))
                    }
                }
            }
        }
    }
}
