package com.youtubestream.app.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun parse_basicSyncedLines() {
        val lines = LrcParser.parse("[00:12.00]Hello\n[00:15.50]World")
        assertEquals(listOf(LyricLine(12_000, "Hello"), LyricLine(15_500, "World")), lines)
    }

    @Test
    fun parse_threeDigitMillis() {
        assertEquals(listOf(LyricLine(12_345, "Hi")), LrcParser.parse("[00:12.345]Hi"))
    }

    @Test
    fun parse_noFraction() {
        assertEquals(listOf(LyricLine(62_000, "X")), LrcParser.parse("[01:02]X"))
    }

    @Test
    fun parse_multipleTimestampsPerLine_expandsAndSorts() {
        assertEquals(
            listOf(LyricLine(12_000, "Yo"), LyricLine(15_000, "Yo")),
            LrcParser.parse("[00:12.00][00:15.00]Yo"),
        )
    }

    @Test
    fun parse_skipsMetadataTags() {
        val lines = LrcParser.parse("[ar:Artist]\n[ti:Title]\n[offset:+200]\n[00:01.00]Go")
        assertEquals(listOf(LyricLine(1_000, "Go")), lines)
    }

    @Test
    fun parse_sortsUnordered() {
        assertEquals(
            listOf(LyricLine(10_000, "A"), LyricLine(20_000, "B")),
            LrcParser.parse("[00:20.00]B\n[00:10.00]A"),
        )
    }

    @Test
    fun parse_keepsEmptyTextLine_andBlankInputIsEmpty() {
        assertEquals(listOf(LyricLine(5_000, "")), LrcParser.parse("[00:05.00]"))
        assertEquals(emptyList<LyricLine>(), LrcParser.parse(""))
    }
}
