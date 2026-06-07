package com.youtubestream.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class GenreChartTitleTest {
    @Test fun stripsTopPrefixAndMusicVideosSuffix() {
        assertEquals("Country & Americana", cleanGenreChartTitle("Top 50 Country & Americana Music Videos United States"))
        assertEquals("Hip Hop", cleanGenreChartTitle("Top 50 Hip Hop Music Videos United States"))
        assertEquals("Pop", cleanGenreChartTitle("Top 50 Pop Music Videos United States"))
        assertEquals("Hard Rock & Metal", cleanGenreChartTitle("Top 50 Hard Rock & Metal Music Videos United States"))
    }
    @Test fun returnsRawWhenPatternAbsent() {
        assertEquals("Weird Title", cleanGenreChartTitle("Weird Title"))
        assertEquals("", cleanGenreChartTitle(""))
    }
}
