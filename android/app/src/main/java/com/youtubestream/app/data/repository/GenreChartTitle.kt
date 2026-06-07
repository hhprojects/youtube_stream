package com.youtubestream.app.data.repository

private val TOP_PREFIX = Regex("^Top \\d+ ")
private val MV_SUFFIX = Regex(" Music Videos .*$")

/**
 * "Top 50 Country & Americana Music Videos United States" -> "Country & Americana".
 * Returns the raw title unchanged if the expected pattern isn't present. Pure → JVM-tested.
 */
fun cleanGenreChartTitle(raw: String): String {
    val stripped = raw.replace(TOP_PREFIX, "").replace(MV_SUFFIX, "")
    return stripped.ifBlank { raw }
}
