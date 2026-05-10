package com.popcorn.live.ui.player

import com.popcorn.live.xtream.XtreamUrlFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackUrlFactoryTest {
    @Test
    fun createsHlsOnlyUrlsForLivePlayback() {
        val urls = PlaybackUrlFactory(
            XtreamUrlFactory("https://iptv.example.com", "Mike", "1234"),
        ).liveUrlsFor(streamId = 42)

        assertEquals("https://iptv.example.com/live/Mike/1234/42.m3u8", urls.preferred)
        assertEquals(urls.preferred, urls.fallback)
    }

    @Test
    fun createsMoviePlaybackUrl() {
        val urls = PlaybackUrlFactory(
            XtreamUrlFactory("https://iptv.example.com", "Mike", "1234"),
        ).movieUrlsFor(streamId = 42, extension = "mkv")

        assertEquals("https://iptv.example.com/movie/Mike/1234/42.mkv", urls.preferred)
        assertEquals(urls.preferred, urls.fallback)
    }

    @Test
    fun createsSeriesPlaybackUrl() {
        val urls = PlaybackUrlFactory(
            XtreamUrlFactory("https://iptv.example.com", "Mike", "1234"),
        ).seriesUrlsFor(episodeId = "episode/42", extension = "mp4")

        assertEquals("https://iptv.example.com/series/Mike/1234/episode%2F42.mp4", urls.preferred)
        assertEquals(urls.preferred, urls.fallback)
    }
}
