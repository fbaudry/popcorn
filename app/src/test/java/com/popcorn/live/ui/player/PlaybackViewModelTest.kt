package com.popcorn.live.ui.player

import com.popcorn.live.catalog.LiveChannel
import com.popcorn.live.catalog.MediaItem
import com.popcorn.live.catalog.MediaKind
import com.popcorn.live.catalog.SeriesEpisode
import com.popcorn.live.user.LibraryItemKind
import com.popcorn.live.user.PlaybackProgress
import com.popcorn.live.user.UserLibraryRepository
import com.popcorn.live.user.UserLibraryStore
import com.popcorn.live.xtream.XtreamUrlFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModelTest {
    @Test
    fun livePlaybackUsesHlsOnlyUrlAndRecordsLastChannel() = runTest {
        withMainDispatcher { dispatcher ->
            val store = RecordingUserLibraryStore()
            val viewModel = playbackViewModel(store, dispatcher)

            viewModel.selectLiveChannel(channel(streamId = 42, name = "France 24"))
            advanceUntilIdle()

            assertEquals("https://iptv.example.com/live/Mike/1234/42.m3u8", viewModel.selectedPlayback.value?.urls?.url)
            assertEquals("42", store.lastPlayback.value[LibraryItemKind.Live])
        }
    }

    @Test
    fun switchLiveChannelMovesThroughVisibleChannels() = runTest {
        withMainDispatcher { dispatcher ->
            val store = RecordingUserLibraryStore()
            val viewModel = playbackViewModel(store, dispatcher)
            val channels = listOf(
                channel(streamId = 1, name = "One"),
                channel(streamId = 2, name = "Two"),
                channel(streamId = 3, name = "Three"),
            )

            viewModel.selectLiveChannel(channels[0])
            viewModel.switchLiveChannel(offset = 1, visibleChannels = channels)
            advanceUntilIdle()

            assertEquals(2, viewModel.selectedPlayback.value?.liveChannelId)
            assertEquals("2", store.lastPlayback.value[LibraryItemKind.Live])
        }
    }

    @Test
    fun movieResumeBuildsPlaybackAndRecordsProgress() = runTest {
        withMainDispatcher { dispatcher ->
            val store = RecordingUserLibraryStore()
            val viewModel = playbackViewModel(store, dispatcher)
            val progress = playbackProgress(
                kind = LibraryItemKind.Movies,
                itemId = "42",
                title = "Movie One",
                containerExtension = "mkv",
                positionMillis = 90_000L,
            )

            viewModel.selectResume(progress)
            viewModel.recordPlaybackProgress(positionMillis = 120_000L, durationMillis = 240_000L)
            advanceUntilIdle()

            assertEquals("https://iptv.example.com/movie/Mike/1234/42.mkv", viewModel.selectedPlayback.value?.urls?.url)
            assertEquals(90_000L, viewModel.selectedPlayback.value?.startPositionMillis)
            assertEquals(120_000L, store.progress.value.getValue(LibraryItemKind.Movies).single().positionMillis)
        }
    }

    @Test
    fun seriesEpisodeSelectionBuildsPlaybackTarget() = runTest {
        withMainDispatcher { dispatcher ->
            val store = RecordingUserLibraryStore()
            val viewModel = playbackViewModel(store, dispatcher)
            val series = MediaItem(
                id = 99,
                kind = MediaKind.Series,
                name = "Show One",
                categoryId = "shows",
                posterUrl = "https://example.com/show.jpg",
                plot = null,
                genre = null,
                rating = null,
                releaseDate = null,
                containerExtension = null,
                sortOrder = 0,
            )
            val episode = SeriesEpisode(
                id = "episode/42",
                title = "Pilot",
                season = 1,
                episodeNumber = 1,
                containerExtension = "mp4",
            )

            viewModel.selectEpisode(series, episode)
            viewModel.recordPlaybackProgress(positionMillis = 60_000L, durationMillis = 120_000L)
            advanceUntilIdle()

            assertEquals(
                "https://iptv.example.com/series/Mike/1234/episode%2F42.mp4",
                viewModel.selectedPlayback.value?.urls?.url,
            )
            assertEquals("99", store.progress.value.getValue(LibraryItemKind.Series).single().parentId)
        }
    }

    @Test
    fun invalidMovieResumeClearsPlayback() = runTest {
        withMainDispatcher { dispatcher ->
            val store = RecordingUserLibraryStore()
            val viewModel = playbackViewModel(store, dispatcher)

            viewModel.selectResume(
                playbackProgress(
                    kind = LibraryItemKind.Movies,
                    itemId = "not-an-int",
                    title = "Broken Resume",
                ),
            )

            assertNull(viewModel.selectedPlayback.value)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.withMainDispatcher(
    block: suspend TestScope.(TestDispatcher) -> Unit,
) {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    try {
        block(dispatcher)
    } finally {
        Dispatchers.resetMain()
    }
}

private fun playbackViewModel(
    store: RecordingUserLibraryStore,
    dispatcher: TestDispatcher,
) = PlaybackViewModel(
    xtreamUrlFactory = XtreamUrlFactory(
        baseUrl = "https://iptv.example.com",
        username = "Mike",
        password = "1234",
    ),
    userLibraryRepository = UserLibraryRepository(store),
    dispatcher = dispatcher,
)

private fun channel(
    streamId: Int,
    name: String,
) = LiveChannel(
    streamId = streamId,
    name = name,
    categoryId = "news",
    streamIcon = null,
    streamType = "live",
    added = null,
    sortOrder = streamId,
)

private fun playbackProgress(
    kind: LibraryItemKind,
    itemId: String,
    title: String,
    containerExtension: String? = null,
    positionMillis: Long = 30_000L,
) = PlaybackProgress(
    kind = kind,
    itemId = itemId,
    parentId = null,
    title = title,
    imageUrl = null,
    containerExtension = containerExtension,
    positionMillis = positionMillis,
    durationMillis = 180_000L,
    updatedAtMillis = 1L,
)

private class RecordingUserLibraryStore : UserLibraryStore {
    val favorites = MutableStateFlow<Map<LibraryItemKind, Set<String>>>(emptyMap())
    val progress = MutableStateFlow<Map<LibraryItemKind, List<PlaybackProgress>>>(emptyMap())
    val lastPlayback = MutableStateFlow<Map<LibraryItemKind, String>>(emptyMap())

    override fun favorites(kind: LibraryItemKind): Flow<Set<String>> =
        favorites.map { current -> current[kind].orEmpty() }

    override fun progress(kind: LibraryItemKind): Flow<List<PlaybackProgress>> =
        progress.map { current -> current[kind].orEmpty() }

    override fun lastPlayback(kind: LibraryItemKind): Flow<String?> =
        lastPlayback.map { current -> current[kind] }

    override suspend fun isFavorite(kind: LibraryItemKind, itemId: String): Boolean =
        favorites.value[kind]?.contains(itemId) == true

    override suspend fun setFavorite(kind: LibraryItemKind, itemId: String, favorite: Boolean) {
        val current = favorites.value
        val nextFavorites = if (favorite) {
            current[kind].orEmpty() + itemId
        } else {
            current[kind].orEmpty() - itemId
        }
        favorites.value = current + (kind to nextFavorites)
    }

    override suspend fun recordProgress(progress: PlaybackProgress) {
        val current = this.progress.value
        val nextProgress = current[progress.kind].orEmpty()
            .filterNot { item -> item.itemId == progress.itemId } + progress
        this.progress.value = current + (progress.kind to nextProgress)
    }

    override suspend fun recordLastPlayback(kind: LibraryItemKind, itemId: String) {
        lastPlayback.value = lastPlayback.value + (kind to itemId)
    }
}
