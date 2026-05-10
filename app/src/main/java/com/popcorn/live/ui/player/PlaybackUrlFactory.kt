package com.popcorn.live.ui.player

import com.popcorn.live.xtream.XtreamUrlFactory

data class PlaybackUrls(
    val url: String,
)

class PlaybackUrlFactory(
    private val xtreamUrlFactory: XtreamUrlFactory,
) {
    fun urlsFor(streamId: Int): PlaybackUrls = liveUrlsFor(streamId)

    fun liveUrlsFor(streamId: Int): PlaybackUrls {
        val hls = xtreamUrlFactory.hlsPlaybackUrl(streamId)

        return PlaybackUrls(url = hls)
    }

    fun movieUrlsFor(streamId: Int, extension: String?): PlaybackUrls {
        val playbackUrl = xtreamUrlFactory.moviePlaybackUrl(
            streamId = streamId,
            extension = extension.orEmpty(),
        )
        return PlaybackUrls(url = playbackUrl)
    }

    fun seriesUrlsFor(episodeId: String, extension: String?): PlaybackUrls {
        val playbackUrl = xtreamUrlFactory.seriesPlaybackUrl(
            episodeId = episodeId,
            extension = extension.orEmpty(),
        )
        return PlaybackUrls(url = playbackUrl)
    }
}
