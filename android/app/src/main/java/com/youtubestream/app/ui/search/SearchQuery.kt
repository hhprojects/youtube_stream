package com.youtubestream.app.ui.search

/**
 * Normalizes a raw search query for use + storage: trims the ends and collapses any run of internal
 * whitespace to a single space. Returns null for blank/whitespace-only input.
 * Keeping this pure (no Android deps) lets the ViewModel reuse it and lets it unit-test on the JVM.
 */
fun normalizeQuery(raw: String): String? {
    val collapsed = raw.trim().replace(Regex("\\s+"), " ")
    return collapsed.ifEmpty { null }
}
