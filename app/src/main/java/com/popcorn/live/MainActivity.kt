package com.popcorn.live

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.popcorn.live.catalog.MediaKind
import com.popcorn.live.ui.config.ConfigScreen
import com.popcorn.live.ui.config.ConfigViewModel
import com.popcorn.live.ui.live.LiveScreen
import com.popcorn.live.ui.live.LiveViewModel
import com.popcorn.live.ui.media.MediaScreen
import com.popcorn.live.ui.media.MoviesViewModel
import com.popcorn.live.ui.media.SeriesViewModel
import com.popcorn.live.ui.navigation.AppSection
import com.popcorn.live.ui.player.LiveChannelControls
import com.popcorn.live.ui.player.PlaybackViewModel
import com.popcorn.live.ui.player.PlayerScreen
import com.popcorn.live.ui.theme.PopcornTheme

class MainActivity : ComponentActivity() {
    private val configViewModel by viewModels<ConfigViewModel> {
        val container = (application as PopcornApplication).container
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ConfigViewModel(
                    configRepository = container.xtreamConnectionConfigRepository,
                    saveConfig = container::saveXtreamConnectionConfig,
                ) as T
            }
        }
    }

    private val liveViewModel by viewModels<LiveViewModel> {
        val container = (application as PopcornApplication).container
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val services = container.requireXtreamServices()
                @Suppress("UNCHECKED_CAST")
                return LiveViewModel(
                    repository = services.liveCatalogRepository,
                    userLibraryRepository = container.userLibraryRepository,
                ) as T
            }
        }
    }

    private val moviesViewModel by viewModels<MoviesViewModel> {
        val container = (application as PopcornApplication).container
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val services = container.requireXtreamServices()
                @Suppress("UNCHECKED_CAST")
                return MoviesViewModel(
                    repository = services.mediaCatalogRepository,
                    userLibraryRepository = container.userLibraryRepository,
                ) as T
            }
        }
    }

    private val seriesViewModel by viewModels<SeriesViewModel> {
        val container = (application as PopcornApplication).container
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val services = container.requireXtreamServices()
                @Suppress("UNCHECKED_CAST")
                return SeriesViewModel(
                    repository = services.mediaCatalogRepository,
                    userLibraryRepository = container.userLibraryRepository,
                ) as T
            }
        }
    }

    private val playbackViewModel by viewModels<PlaybackViewModel> {
        val container = (application as PopcornApplication).container
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val services = container.requireXtreamServices()
                @Suppress("UNCHECKED_CAST")
                return PlaybackViewModel(
                    xtreamUrlFactory = services.xtreamUrlFactory,
                    userLibraryRepository = container.userLibraryRepository,
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PopcornTheme {
                val configState by configViewModel.uiState.collectAsStateWithLifecycle()
                if (!configState.isConfigured) {
                    ConfigScreen(
                        state = configState,
                        onBaseUrlChanged = configViewModel::onBaseUrlChanged,
                        onUsernameChanged = configViewModel::onUsernameChanged,
                        onPasswordChanged = configViewModel::onPasswordChanged,
                        onSave = configViewModel::save,
                    )
                } else {
                    PopcornAppContent(
                        liveViewModel = liveViewModel,
                        moviesViewModel = moviesViewModel,
                        seriesViewModel = seriesViewModel,
                        playbackViewModel = playbackViewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun PopcornAppContent(
    liveViewModel: LiveViewModel,
    moviesViewModel: MoviesViewModel,
    seriesViewModel: SeriesViewModel,
    playbackViewModel: PlaybackViewModel,
) {
    var selectedSection by rememberSaveable { mutableStateOf(AppSection.Live) }
    val liveState by liveViewModel.uiState.collectAsStateWithLifecycle()
    val playback by playbackViewModel.selectedPlayback.collectAsStateWithLifecycle()

    val onSectionSelected: (AppSection) -> Unit = { section ->
        selectedSection = section
        playbackViewModel.closePlayback()
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

    val selectedPlayback = playback
    if (selectedPlayback != null) {
        val liveChannelControls = selectedPlayback.liveChannelId?.let {
            LiveChannelControls(
                canChangeChannel = liveState.visibleChannels.size > 1,
                onPreviousChannel = { playbackViewModel.switchLiveChannel(-1, liveState.visibleChannels) },
                onNextChannel = { playbackViewModel.switchLiveChannel(1, liveState.visibleChannels) },
            )
        }
        PlayerScreen(
            content = selectedPlayback.content,
            urls = selectedPlayback.urls,
            onBack = playbackViewModel::closePlayback,
            liveChannelControls = liveChannelControls,
            startPositionMillis = selectedPlayback.startPositionMillis,
            onPlaybackProgress = playbackViewModel::recordPlaybackProgress,
        )
        return
    }

    when (selectedSection) {
        AppSection.Live -> {
            LiveScreen(
                state = liveState,
                selectedSection = selectedSection,
                onSectionSelected = onSectionSelected,
                onMenuItemSelected = liveViewModel::onMenuItemSelected,
                onSearchChanged = liveViewModel::onSearchChanged,
                onRefresh = { liveViewModel.refresh() },
                onChannelSelected = playbackViewModel::selectLiveChannel,
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
                    playbackViewModel.selectMovie(
                        movie = movie,
                        progress = moviesState.progressByItemId[movie.id.toString()],
                    )
                },
                onFavoriteToggled = moviesViewModel::toggleFavorite,
                onResumeSelected = playbackViewModel::selectResume,
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
                onResumeSelected = playbackViewModel::selectResume,
                onBackFromDetails = seriesViewModel::clearSeriesSelection,
                onEpisodeSelected = { series, episode ->
                    playbackViewModel.selectEpisode(
                        series = series,
                        episode = episode,
                        progress = seriesState.progressByItemId[episode.id],
                    )
                },
            )
        }
    }
}
