package com.youtubestream.app.lyrics

/** One timed line of a synced lyric. */
data class LyricLine(val timeMs: Long, val text: String)

/** Pure LRC parsing + current-line selection. Zero Android imports → JVM-unit-testable. */
object LrcParser {
    // [mm:ss], [mm:ss.xx], or [mm:ss.xxx] (some files use ':' as the fraction separator).
    private val TIME = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parse(raw: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        for (line in raw.lineSequence()) {
            val stamps = TIME.findAll(line).toList()
            if (stamps.isEmpty()) continue   // metadata tags ([ar:]/[ti:]/[offset:]) and plain lines: no timestamp
            val text = line.substring(stamps.last().range.last + 1).trim()
            for (m in stamps) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val fracMs = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100   // tenths
                    2 -> frac.toLong() * 10    // centiseconds
                    else -> frac.take(3).toLong()
                }
                out.add(LyricLine(min * 60_000 + sec * 1_000 + fracMs, text))
            }
        }
        out.sortBy { it.timeMs }
        return out
    }
}
