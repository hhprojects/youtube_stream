package com.youtubestream.app.data.settings

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Normalizes a user-entered server URL, or returns null if it can't be a valid http(s) URL.
 * Prepends `http://` when no scheme is given, but otherwise returns the input as-typed — it does NOT
 * canonicalize (no trailing slash added), so the saved value matches what the user expects to see.
 *
 * This guards [com.youtubestream.app.data.remote.BaseUrlInterceptor], which calls `toHttpUrl()` and
 * would otherwise throw on *every* request for a malformed value (e.g. a host with no scheme),
 * bricking networking until the user resets.
 */
fun normalizeServerUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val withScheme =
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
        else "http://$trimmed"
    return if (withScheme.toHttpUrlOrNull() != null) withScheme else null
}
