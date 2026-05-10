package com.popcorn.live

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.popcorn.live.catalog.LiveChannel
import com.popcorn.live.catalog.MediaItem
import com.popcorn.live.catalog.MediaKind
import com.popcorn.live.catalog.SeriesEpisode
import com.popcorn.live.ui.live.LiveScreen
import com.popcorn.live.ui.live.LiveViewModel
import com.popcorn.live.ui.media.MediaScreen
import com.popcorn.live.ui.media.MoviesViewModel
import com.popcorn.live.ui.media.SeriesViewModel
import com.popcorn.live.ui.navigation.AppSection
import com.popcorn.live.ui.player.LiveChannelControls
import com.popcorn.live.ui.player.PlaybackUrlFactory
import com.popcorn.live.ui.player.PlaybackUrls
import com.popcorn.live.ui.player.PlayerContent
import com.popcorn.live.ui.player.PlayerScreen
import com.popcorn.live.ui.theme.PopcornTheme
import com.popcorn.live.user.LibraryItemKind
import com.popcorn.live.user.PlaybackProgress
import com.popcorn.live.user.toLibraryItemKind
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val liveViewModel by viewModels<LiveViewModel> {
        val container = (application as PopcornApplication).container
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LiveViewModel(
                    repository = container.liveCatalogRepository,
                    userLibraryRepository = container.userLibraryRepository,
                ) as T
            }
        }
    }
    private val moviesViewModel by viewModels<MoviesViewModel> {
        val container = (application as PopcornApplication).container
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MoviesViewModel(
                    repository = container.mediaCatalogRepository,
                    userLibraryRepository = container.userLibraryRepository,
                ) as T
            }
        }
    }
    private val seriesViewModel by viewModels<SeriesViewModel> {
        val container = (application as PopcornApplication).container
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SeriesViewModel(
                    repository = container.mediaCatalogRepository,
                    userLibraryRepository = container.userLibraryRepository,
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PopcornTheme {
                var selectedSection by remember { mutableStateOf(AppSection.Live) }
                var selectedPlayback by remember { mutableStateOf<SelectedPlayback?>(null) }
                val container = remember { (application as PopcornApplication).container }
                val userLibraryRepository = container.userLibraryRepository
                val playbackUrlFactory = remember {
                    PlaybackUrlFactory(container.xtreamUrlFactory)
                }
                val liveState by liveViewModel.uiState.collectAsStateWithLifecycle()

                fun playbackForLiveChannel(channel: LiveChannel): SelectedPlayback {
                    return SelectedPlayback(
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

                fun selectLiveChannel(channel: LiveChannel) {
                    liveViewModel.recordLastLiveChannel(channel)
                    selectedPlayback = playbackForLiveChannel(channel)
                }

                fun playbackForMovie(
                    movie: MediaItem,
                    progress: PlaybackProgress? = null,
                ): SelectedPlayback {
                    return SelectedPlayback(
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

                fun playbackForEpisode(
                    series: MediaItem,
                    episode: SeriesEpisode,
                    progress: PlaybackProgress? = null,
                ): SelectedPlayback {
                    return SelectedPlayback(
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

                fun playbackForResume(progress: PlaybackProgress): SelectedPlayback? {
                    return when (progress.kind) {
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

                fun switchLiveChannel(offset: Int) {
                    val currentChannelId = selectedPlayback?.liveChannelId ?: return
                    val channels = liveState.visibleChannels
                    if (channels.size < 2) {
                        return
                    }

                    val currentIndex = channels.indexOfFirst { channel ->
                        channel.streamId == currentChannelId
                    }
                    if (currentIndex == -1) {
                        return
                    }

                    val nextIndex = (currentIndex + offset + channels.size) % channels.size
                    selectLiveChannel(channels[nextIndex])
                }

                val onSectionSelected: (AppSection) -> Unit = { section ->
                    selectedSection = section
                    selectedPlayback = null
                    when (section) {
                        AppSection.Live -> {
                            liveViewModel.selectFavorites()
                            seriesViewModel.clearSeriesSelection()
                        }
                        AppSection.Movies -> {
                            moviesViewModel.selectFavorites()
                            seriesViewModel.clearSeriesSelection()
                        }
                        AppSection.Series -> {
                            seriesViewModel.selectFavorites()
                        }
                    }
                }

                val playback = selectedPlayback
                if (playback != null) {
                    val liveChannelControls = playback.liveChannelId?.let {
                        LiveChannelControls(
                            canChangeChannel = liveState.visibleChannels.size > 1,
                            onPreviousChannel = { switchLiveChannel(-1) },
                            onNextChannel = { switchLiveChannel(1) },
                        )
                    }
                    PlayerScreen(
                        content = playback.content,
                        urls = playback.urls,
                        onBack = { selectedPlayback = null },
                        liveChannelControls = liveChannelControls,
                        startPositionMillis = playback.startPositionMillis,
                        onPlaybackProgress = { positionMillis, durationMillis ->
                            playback.progressTarget?.let { target ->
                                lifecycleScope.launch {
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
                        },
                    )
                } else {
                    when (selectedSection) {
                        AppSection.Live -> {
                            LiveScreen(
                                state = liveState,
                                selectedSection = selectedSection,
                                onSectionSelected = onSectionSelected,
                                onMenuItemSelected = liveViewModel::onMenuItemSelected,
                                onSearchChanged = liveViewModel::onSearchChanged,
                                onRefresh = { liveViewModel.refresh() },
                                onChannelSelected = ::selectLiveChannel,
                                onFavoriteToggled = liveViewModel::toggleFavorite,
                            )
                        }

                        AppSection.Movies -> {
                            val moviesState by moviesViewModel.uiState.collectAsStateWithLifecycle()
                            MediaScreen(
                                section = AppSection.Movies,
                                kind = MediaKind.Movies,
                                state = moviesState,
                                selectedSection = selectedSection,
                                onSectionSelected = onSectionSelected,
                                onMenuItemSelected = moviesViewModel::onMenuItemSelected,
                                onSearchChanged = moviesViewModel::onSearchChanged,
                                onRefresh = { moviesViewModel.refresh() },
                                onItemSelected = { movie ->
                                    selectedPlayback = playbackForMovie(
                                        movie = movie,
                                        progress = moviesState.progressByItemId[movie.id.toString()],
                                    )
                                },
                                onFavoriteToggled = moviesViewModel::toggleFavorite,
                                onResumeSelected = { progress ->
                                    playbackForResume(progress)?.let { playback ->
                                        selectedPlayback = playback
                                    }
                                },
                                onBackFromDetails = {},
                                onEpisodeSelected = { _, _ -> },
                            )
                        }

                        AppSection.Series -> {
                            val seriesState by seriesViewModel.uiState.collectAsStateWithLifecycle()
                            MediaScreen(
                                section = AppSection.Series,
                                kind = MediaKind.Series,
                                state = seriesState,
                                selectedSection = selectedSection,
                                onSectionSelected = onSectionSelected,
                                onMenuItemSelected = seriesViewModel::onMenuItemSelected,
                                onSearchChanged = seriesViewModel::onSearchChanged,
                                onRefresh = { seriesViewModel.refresh() },
                                onItemSelected = seriesViewModel::onSeriesSelected,
                                onFavoriteToggled = seriesViewModel::toggleFavorite,
                                onResumeSelected = { progress ->
                                    playbackForResume(progress)?.let { playback ->
                                        selectedPlayback = playback
                                    }
                                },
                                onBackFromDetails = seriesViewModel::clearSeriesSelection,
                                onEpisodeSelected = { series, episode ->
                                    selectedPlayback = playbackForEpisode(
                                        series = series,
                                        episode = episode,
                                        progress = seriesState.progressByItemId[episode.id],
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

}

private data class SelectedPlayback(
    val content: PlayerContent,
    val urls: PlaybackUrls,
    val liveChannelId: Int? = null,
    val startPositionMillis: Long = 0L,
    val progressTarget: PlaybackProgressTarget? = null,
)

private data class PlaybackProgressTarget(
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
