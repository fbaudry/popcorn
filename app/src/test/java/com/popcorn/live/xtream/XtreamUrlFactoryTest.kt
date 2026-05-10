package com.popcorn.live.xtream

import org.junit.Assert.assertEquals
import org.junit.Test

class XtreamUrlFactoryTest {
    private val factory = XtreamUrlFactory(
        baseUrl = "https://iptv.example.com:8080/",
        username = "Mike",
        password = "1234",
    )

    @Test
    fun accountInfoUrlUsesPlayerApiWithoutAction() {
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234",
            factory.accountInfoUrl(),
        )
    }

    @Test
    fun liveCategoriesUrlUsesGetLiveCategoriesAction() {
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234&action=get_live_categories",
            factory.liveCategoriesUrl(),
        )
    }

    @Test
    fun liveStreamsUrlCanTargetOneCategory() {
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234&action=get_live_streams&category_id=25",
            factory.liveStreamsUrl(categoryId = "25"),
        )
    }

    @Test
    fun liveStreamsUrlCanOmitCategory() {
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234&action=get_live_streams",
            factory.liveStreamsUrl(),
        )
    }

    @Test
    fun vodUrlsUseExpectedXtreamActions() {
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234&action=get_vod_categories",
            factory.vodCategoriesUrl(),
        )
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234&action=get_vod_streams&category_id=movies",
            factory.vodStreamsUrl(categoryId = "movies"),
        )
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234&action=get_vod_info&vod_id=987",
            factory.vodInfoUrl(vodId = 987),
        )
    }

    @Test
    fun seriesUrlsUseExpectedXtreamActions() {
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234&action=get_series_categories",
            factory.seriesCategoriesUrl(),
        )
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234&action=get_series&category_id=shows",
            factory.seriesUrl(categoryId = "shows"),
        )
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike&password=1234&action=get_series_info&series_id=654",
            factory.seriesInfoUrl(seriesId = 654),
        )
    }

    @Test
    fun playbackUrlsUseLivePathAndStreamId() {
        assertEquals(
            "https://iptv.example.com:8080/live/Mike/1234/55555.m3u8",
            factory.hlsPlaybackUrl(streamId = 55555),
        )
    }

    @Test
    fun mediaPlaybackUrlsUseMovieAndSeriesPaths() {
        assertEquals(
            "https://iptv.example.com:8080/movie/Mike/1234/42.mkv",
            factory.moviePlaybackUrl(streamId = 42, extension = "mkv"),
        )
        assertEquals(
            "https://iptv.example.com:8080/series/Mike/1234/abc%2F42.mp4",
            factory.seriesPlaybackUrl(episodeId = "abc/42", extension = ""),
        )
    }

    @Test
    fun urlsTrimBaseUrlAndEncodeDynamicSegments() {
        val encodedFactory = XtreamUrlFactory(
            baseUrl = " https://iptv.example.com:8080/// ",
            username = "Mike Space",
            password = "p@ss/word?",
        )

        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike%20Space&password=p%40ss%2Fword%3F",
            encodedFactory.accountInfoUrl(),
        )
        assertEquals(
            "https://iptv.example.com:8080/player_api.php?username=Mike%20Space&password=p%40ss%2Fword%3F&action=get_live_streams&category_id=news%20%26%20sport%2F25",
            encodedFactory.liveStreamsUrl(categoryId = "news & sport/25"),
        )
        assertEquals(
            "https://iptv.example.com:8080/live/Mike%20Space/p%40ss%2Fword%3F/55555.m3u8",
            encodedFactory.hlsPlaybackUrl(streamId = 55555),
        )
    }
}
