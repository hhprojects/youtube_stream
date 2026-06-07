package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.RecentSearch
import com.youtubestream.app.data.local.RecentSearchDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Thin seam over [RecentSearchDao] so callers see plain query strings, never the Room entity. */
class RecentSearchRepository(private val dao: RecentSearchDao) {

    /** Newest-first recent query strings, capped at [limit]. */
    fun observe(limit: Int): Flow<List<String>> =
        dao.recent(limit).map { rows -> rows.map { it.query } }

    /** Records a (normalized) query; REPLACE on the PK dedups and bumps recency. */
    suspend fun save(query: String) = dao.upsert(RecentSearch(query, System.currentTimeMillis()))

    suspend fun remove(query: String) = dao.delete(query)

    suspend fun clear() = dao.clear()
}
