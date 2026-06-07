package com.youtubestream.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One remembered search query. The (normalized) text is the primary key, so re-searching the same
 * term REPLACEs the row and bumps [usedAt] — that's how dedup + recency happen without extra code.
 * Tiny table (capped on read), so no index is needed.
 */
@Entity(tableName = "recent_searches")
data class RecentSearch(
    @PrimaryKey val query: String,
    val usedAt: Long,   // epoch millis of the most recent use
)
