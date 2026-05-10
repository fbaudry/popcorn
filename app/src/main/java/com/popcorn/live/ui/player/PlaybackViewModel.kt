package com.popcorn.live.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorn.live.catalog.LiveChannel
import com.popcorn.live.catalog.MediaItem
import com.popcorn.live.catalog.MediaKind
import com.popcorn.live.catalog.SeriesEpisode
import com.popcorn.live.user.LibraryItemKind
import com.popcorn.live.user.PlaybackProgress
import com.popcorn.live.user.UserLibraryRepository
import com.popcorn.live.user.toLibraryItemKind
import com.popcorn.live.xtream.XtreamUrlFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackViewModel(
    xtreamUrlFactory: XtreamUrlFactory,
    private val userLibraryRepository: UserLibraryRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val playbackUrlFactory = PlaybackUrlFactory(xtreamUrlFactory)
    private val _selectedPlayback = MutableStateFlow<SelectedPlayback?>(null)

    val selectedPlayback: StateFlow<SelectedPlayback?> = _selectedPlayback.asStateFlow()

    fun selectLiveChannel(channel: LiveChannel) {
        _selectedPlayback.value = playbackForLiveChannel(channel)
        viewModelScope.launch {
            withContext(dispatcher) {
                userLibraryRepository.recordLastLiveChannel(channel.streamId)
            }
        }
    }

    fun selectMovie(
        movie: MediaItem,
        progress: PlaybackProgress? = null,
    ) {
        _selectedPlayback.value = SelectedPlayback(
            content = PlayerContent(
                key = "movie-${movie.id}",
                title = movie.name,
                imageUrl = movie.posterUrl,
                label = "FILM",
            ),
            urls = playbackUrlFactory.movieUrlsFor(
                streamId = movie.id,
                extension = movie.containerExtension,
            ),
            startPositionMillis = progress?.positionMillis ?: 0L,
            progressTarget = PlaybackProgressTarget(
                kind = movie.kind.toLibraryItemKind(),
                itemId = movie.id.toString(),
                parentId = null,
                title = movie.name,
                imageUrl = movie.posterUrl,
                containerExtension = movie.containerExtension,
            ),
        )
    }

    fun selectEpisode(
        series: MediaItem,
        episode: SeriesEpisode,
        progress: PlaybackProgress? = null,
    ) {
        _selectedPlayback.value = SelectedPlayback(
            content = PlayerContent(
                key = "series-${episode.id}",
                title = "${series.name} - ${episode.title}",
                imageUrl = series.posterUrl,
                label = "ÉPISODE",
            ),
            urls = playbackUrlFactory.seriesUrlsFor(
                episodeId = episode.id,
                extension = episode.containerExtension,
            ),
            startPositionMillis = progress?.positionMillis ?: 0L,
            progressTarget = PlaybackProgressTarget(
                kind = MediaKind.Series.toLibraryItemKind(),
                itemId = episode.id,
                parentId = series.id.toString(),
                title = "${series.name} - ${episode.title}",
                imageUrl = series.posterUrl,
                containerExtension = episode.containerExtension,
            ),
        )
    }

    fun selectResume(progress: PlaybackProgress) {
        _selectedPlayback.value = when (progress.kind) {
            LibraryItemKind.Movies -> progress.itemId.toIntOrNull()?.let { streamId ->
                SelectedPlayback(
                    content = PlayerContent(
                        key = "resume-movie-${progress.itemId}",
                        title = progress.title,
                        imageUrl = progress.imageUrl,
                        label = "FILM",
                    ),
                    urls = playbackUrlFactory.movieUrlsFor(
                        streamId = streamId,
                        extension = progress.containerExtension,
                    ),
                    startPositionMillis = progress.positionMillis,
                    progressTarget = progress.toTarget(),
                )
            }
            LibraryItemKind.Series -> SelectedPlayback(
                content = PlayerContent(
                    key = "resume-series-${progress.itemId}",
                    title = progress.title,
                    imageUrl = progress.imageUrl,
                    label = "ÉPISODE",
                ),
                urls = playbackUrlFactory.seriesUrlsFor(
                    episodeId = progress.itemId,
                    extension = progress.containerExtension,
                ),
                startPositionMillis = progress.positionMillis,
                progressTarget = progress.toTarget(),
            )
            LibraryItemKind.Live -> null
        }
    }

    fun switchLiveChannel(offset: Int, visibleChannels: List<LiveChannel>) {
        val currentChannelId = _selectedPlayback.value?.liveChannelId ?: return
        if (visibleChannels.size < 2) {
            return
        }

        val currentIndex = visibleChannels.indexOfFirst { channel ->
            channel.streamId == currentChannelId
        }
        if (currentIndex == -1) {
            return
        }

        val nextIndex = (currentIndex + offset + visibleChannels.size) % visibleChannels.size
        selectLiveChannel(visibleChannels[nextIndex])
    }

    fun closePlayback() {
        _selectedPlayback.value = null
    }

    fun recordPlaybackProgress(positionMillis: Long, durationMillis: Long) {
        val target = _selectedPlayback.value?.progressTarget ?: return
        viewModelScope.launch {
            withContext(dispatcher) {
                userLibraryRepository.recordProgress(
                    kind = target.kind,
                    itemId = target.itemId,
                    parentId = target.parentId,
                    title = target.title,
                    imageUrl = target.imageUrl,
                    containerExtension = target.containerExtension,
                    positionMillis = positionMillis,
                    durationMillis = durationMillis,
                )
            }
        }
    }

    private fun playbackForLiveChannel(channel: LiveChannel): SelectedPlayback =
        SelectedPlayback(
            content = PlayerContent(
                key = "live-${channel.streamId}",
                title = channel.name,
                imageUrl = channel.streamIcon,
                label = "LIVE",
            ),
            urls = playbackUrlFactory.liveUrlsFor(streamId = channel.streamId),
            liveChannelId = channel.streamId,
        )
}

data class SelectedPlayback(
    val content: PlayerContent,
    val urls: PlaybackUrls,
    val liveChannelId: Int? = null,
    val startPositionMillis: Long = 0L,
    val progressTarget: PlaybackProgressTarget? = null,
)

data class PlaybackProgressTarget(
    val kind: LibraryItemKind,
    val itemId: String,
    val parentId: String?,
    val title: String,
    val imageUrl: String?,
    val containerExtension: String?,
)

private fun PlaybackProgress.toTarget() = PlaybackProgressTarget(
    kind = kind,
    itemId = itemId,
    parentId = parentId,
    title = title,
    imageUrl = imageUrl,
    containerExtension = containerExtension,
)
