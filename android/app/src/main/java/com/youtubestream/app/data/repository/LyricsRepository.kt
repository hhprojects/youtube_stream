package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.Lyrics
import com.youtubestream.app.data.local.LyricsDao
import com.youtubestream.app.data.remote.LyricsApi
import com.youtubestream.app.lyrics.LrcParser
import com.youtubestream.app.lyrics.LyricsResult
import com.youtubestream.app.lyrics.SongRef

/**
 * Room-first lyrics lookup. A cached positive is used indefinitely; a cached negative older than the TTL is
 * re-fetched (lrclib's catalogue grows). A Pi/lrclib failure returns [LyricsResult.Unavailable] WITHOUT
 * persisting a negative, so it retries when back online. Glue (Room + Retrofit) — verified by build + device.
 */
class LyricsRepository(
    private val api: LyricsApi,
    private val dao: LyricsDao,
) {
    suspend fun getLyrics(ref: SongRef): LyricsResult {
        dao.get(ref.id)?.let { cached ->
            when {
                cached.syncedLrc != null -> return LyricsResult.Synced(LrcParser.parse(cached.syncedLrc))
                cached.plain != null -> return LyricsResult.Plain(cached.plain)
                isFresh(cached.fetchedAt) -> return LyricsResult.None   // fresh negative — don't re-hit
                else -> Unit                                            // stale negative — fall through
            }
        }
        val dto = try {
            api.lyrics(ref.id, ref.title, ref.artist, (ref.durationMs / 1000).toInt())
        } catch (e: Exception) {
            return LyricsResult.Unavailable   // unreachable: do NOT persist a negative
        }
        dao.upsert(Lyrics(ref.id, dto.synced, dto.plain, System.currentTimeMillis()))
        return when {
            dto.synced != null -> LyricsResult.Synced(LrcParser.parse(dto.synced))
            dto.plain != null -> LyricsResult.Plain(dto.plain)
            else -> LyricsResult.None
        }
    }

    private fun isFresh(fetchedAt: Long): Boolean =
        System.currentTimeMillis() - fetchedAt < NEGATIVE_TTL_MS

    private companion object {
        const val NEGATIVE_TTL_MS = 14L * 24 * 60 * 60 * 1000   // re-check "no lyrics" after 14 days
    }
}
