package com.popcorn.live.ui.live

import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.LiveCatalogRepository
import com.popcorn.live.catalog.LiveCatalogStore
import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel
import com.popcorn.live.test.fakeUserLibraryRepository
import com.popcorn.live.ui.navigation.CatalogMenuIds
import com.popcorn.live.user.LibraryItemKind
import com.popcorn.live.xtream.XtreamAccountResponseDto
import com.popcorn.live.xtream.XtreamApi
import com.popcorn.live.xtream.XtreamLiveCategoryDto
import com.popcorn.live.xtream.XtreamLiveStreamDto
import com.popcorn.live.xtream.XtreamMediaCategoryDto
import com.popcorn.live.xtream.XtreamSeriesDto
import com.popcorn.live.xtream.XtreamSeriesInfoResponseDto
import com.popcorn.live.xtream.XtreamVodInfoResponseDto
import com.popcorn.live.xtream.XtreamVodStreamDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveViewModelTest {
    @Test
    fun selectedCategoryDefaultsToFirstCachedCategory() = runTest {
        withMainDispatcher { dispatcher ->
            val categories = listOf(
                LiveCategory("news", "News", 0),
                LiveCategory("sports", "Sports", 1),
            )
            val channels = listOf(
                LiveChannel(1, "France 24", "news", null, "live", null, 0),
                LiveChannel(2, "Eurosport", "sports", null, "live", null, 1),
            )
            val viewModel = LiveViewModel(
                repository = repositoryWith(
                    categories = categories,
                    channels = channels,
                ),
                userLibraryRepository = fakeUserLibraryRepository(),
                dispatcher = dispatcher,
            )

            advanceUntilIdle()

            assertEquals(CatalogMenuIds.FAVORITES, viewModel.uiState.value.selectedMenuItemId)
            assertNull(viewModel.uiState.value.selectedCategoryId)
            assertTrue(viewModel.uiState.value.visibleChannels.isEmpty())

            viewModel.onMenuItemSelected(CatalogMenuIds.category("news"))
            advanceUntilIdle()

            assertEquals("news", viewModel.uiState.value.selectedCategoryId)
            assertEquals(listOf("France 24"), viewModel.uiState.value.visibleChannels.map { it.name })
        }
    }

    @Test
    fun searchFiltersChannelsAcrossCategories() = runTest {
        withMainDispatcher { dispatcher ->
            val viewModel = LiveViewModel(
                repository = repositoryWith(
                    categories = listOf(
                        LiveCategory("news", "News", 0),
                        LiveCategory("sports", "Sports", 1),
                    ),
                    channels = listOf(
                        LiveChannel(1, "France 24", "news", null, "live", null, 0),
                        LiveChannel(2, "Eurosport", "sports", null, "live", null, 1),
                    ),
                ),
                userLibraryRepository = fakeUserLibraryRepository(
                    favorites = mapOf(LibraryItemKind.Live to setOf("1", "2")),
                ),
                dispatcher = dispatcher,
            )

            advanceUntilIdle()
            viewModel.onSearchChanged("euro")
            advanceUntilIdle()

            assertEquals(listOf("Eurosport"), viewModel.uiState.value.visibleChannels.map { it.name })
        }
    }

    @Test
    fun searchSuggestionsComeFromCachedCatalog() = runTest {
        withMainDispatcher { dispatcher ->
            val viewModel = LiveViewModel(
                repository = repositoryWith(
                    categories = listOf(
                        LiveCategory("news", "News", 0),
                        LiveCategory("sports", "Sports", 1),
                    ),
                    channels = listOf(
                        LiveChannel(1, "France 24", "news", null, "live", null, 0),
                        LiveChannel(2, "France 3", "news", null, "live", null, 1),
                        LiveChannel(3, "Eurosport", "sports", null, "live", null, 2),
                    ),
                ),
                userLibraryRepository = fakeUserLibraryRepository(
                    favorites = mapOf(LibraryItemKind.Live to setOf("1", "2", "3")),
                ),
                dispatcher = dispatcher,
            )

            advanceUntilIdle()
            viewModel.onSearchChanged("france")
            advanceUntilIdle()

            assertEquals(
                listOf("France 24", "France 3"),
                viewModel.uiState.value.searchSuggestions,
            )
        }
    }

    @Test
    fun silentRefreshDoesNotSetRefreshingState() = runTest {
        withMainDispatcher { dispatcher ->
            val api = BlockingXtreamApi(
                categories = listOf(LiveCategory("news", "News", 0)),
                channels = listOf(LiveChannel(1, "France 24", "news", null, "live", null, 0)),
                blockedRefreshCall = 1,
            )
            val viewModel = LiveViewModel(
                repository = repositoryWith(
                    categories = listOf(LiveCategory("news", "News", 0)),
                    channels = listOf(LiveChannel(1, "France 24", "news", null, "live", null, 0)),
                    api = api,
                ),
                userLibraryRepository = fakeUserLibraryRepository(),
                dispatcher = dispatcher,
            )

            runCurrent()

            assertFalse(viewModel.uiState.value.isRefreshing)

            api.completeBlockedRefresh()
            advanceUntilIdle()
        }
    }

    @Test
    fun manualRefreshSetsRefreshingStateUntilRefreshCompletes() = runTest {
        withMainDispatcher { dispatcher ->
            val api = BlockingXtreamApi(
                categories = listOf(LiveCategory("news", "News", 0)),
                channels = listOf(LiveChannel(1, "France 24", "news", null, "live", null, 0)),
                blockedRefreshCall = 2,
            )
            val viewModel = LiveViewModel(
                repository = repositoryWith(
                    categories = listOf(LiveCategory("news", "News", 0)),
                    channels = listOf(LiveChannel(1, "France 24", "news", null, "live", null, 0)),
                    api = api,
                ),
                userLibraryRepository = fakeUserLibraryRepository(),
                dispatcher = dispatcher,
            )

            advanceUntilIdle()
            viewModel.refresh()
            runCurrent()

            assertTrue(viewModel.uiState.value.isRefreshing)

            api.completeBlockedRefresh()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRefreshing)
        }
    }

    @Test
    fun refreshErrorIsBlockingOnlyWhenCacheIsEmpty() = runTest {
        withMainDispatcher { dispatcher ->
            val cachedViewModel = LiveViewModel(
                repository = repositoryWith(
                    categories = listOf(LiveCategory("news", "News", 0)),
                    channels = listOf(LiveChannel(1, "France 24", "news", null, "live", null, 0)),
                    api = FailingXtreamApi(),
                ),
                userLibraryRepository = fakeUserLibraryRepository(),
                dispatcher = dispatcher,
            )
            val emptyViewModel = LiveViewModel(
                repository = repositoryWith(
                    categories = emptyList(),
                    channels = emptyList(),
                    api = FailingXtreamApi(),
                ),
                userLibraryRepository = fakeUserLibraryRepository(),
                dispatcher = dispatcher,
            )

            advanceUntilIdle()

            assertNull(cachedViewModel.uiState.value.blockingError)
            assertEquals("network", cachedViewModel.uiState.value.metadata.lastRefreshError)
            assertEquals("network", emptyViewModel.uiState.value.blockingError)
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

private fun repositoryWith(
    categories: List<LiveCategory>,
    channels: List<LiveChannel>,
    metadata: CatalogMetadata = CatalogMetadata(),
    api: XtreamApi = MirroringXtreamApi(categories, channels),
): LiveCatalogRepository =
    LiveCatalogRepository(
        api = api,
        store = FakeLiveCatalogStore(
            initialCategories = categories,
            initialChannels = channels,
            initialMetadata = metadata,
        ),
    )

private class FakeLiveCatalogStore(
    initialCategories: List<LiveCategory> = emptyList(),
    initialChannels: List<LiveChannel> = emptyList(),
    initialMetadata: CatalogMetadata = CatalogMetadata(),
) : LiveCatalogStore {
    override val categories = MutableStateFlow(initialCategories)
    override val channels = MutableStateFlow(initialChannels)
    override val metadata = MutableStateFlow(initialMetadata)

    override suspend fun replaceCatalog(
        categories: List<LiveCategory>,
        channels: List<LiveChannel>,
        refreshedAtMillis: Long,
    ) {
        this.categories.value = categories
        this.channels.value = channels
        metadata.value = CatalogMetadata(lastSuccessfulRefreshAt = refreshedAtMillis)
    }

    override suspend fun recordRefreshError(message: String) {
        metadata.value = metadata.value.copy(lastRefreshError = message)
    }
}

private open class MirroringXtreamApi(
    private val categories: List<LiveCategory>,
    private val channels: List<LiveChannel>,
) : XtreamApi {
    override suspend fun account(): XtreamAccountResponseDto = XtreamAccountResponseDto()

    open override suspend fun liveCategories(): List<XtreamLiveCategoryDto> =
        categories.map { category ->
            XtreamLiveCategoryDto(
                categoryId = category.id,
                categoryName = category.name,
            )
        }

    override suspend fun liveStreams(categoryId: String?): List<XtreamLiveStreamDto> =
        channels
            .filter { channel -> categoryId == null || channel.categoryId == categoryId }
            .map { channel ->
                XtreamLiveStreamDto(
                    num = channel.sortOrder,
                    name = channel.name,
                    streamType = channel.streamType,
                    streamId = channel.streamId,
                    streamIcon = channel.streamIcon,
                    added = channel.added,
                    categoryId = channel.categoryId,
                )
            }

    override suspend fun vodCategories(): List<XtreamMediaCategoryDto> = emptyList()
    override suspend fun vodStreams(categoryId: String?): List<XtreamVodStreamDto> = emptyList()
    override suspend fun vodInfo(vodId: Int): XtreamVodInfoResponseDto = XtreamVodInfoResponseDto()
    override suspend fun seriesCategories(): List<XtreamMediaCategoryDto> = emptyList()
    override suspend fun series(categoryId: String?): List<XtreamSeriesDto> = emptyList()
    override suspend fun seriesInfo(seriesId: Int): XtreamSeriesInfoResponseDto = XtreamSeriesInfoResponseDto()
}

private class BlockingXtreamApi(
    categories: List<LiveCategory>,
    channels: List<LiveChannel>,
    private val blockedRefreshCall: Int,
) : MirroringXtreamApi(categories, channels) {
    private val refreshGate = CompletableDeferred<Unit>()
    private var liveCategoriesCalls = 0

    override suspend fun liveCategories(): List<XtreamLiveCategoryDto> {
        liveCategoriesCalls += 1
        if (liveCategoriesCalls == blockedRefreshCall) {
            refreshGate.await()
        }
        return super.liveCategories()
    }

    fun completeBlockedRefresh() {
        refreshGate.complete(Unit)
    }
}

private class FailingXtreamApi : XtreamApi {
    override suspend fun account(): XtreamAccountResponseDto = error("network")
    override suspend fun liveCategories(): List<XtreamLiveCategoryDto> = error("network")
    override suspend fun liveStreams(categoryId: String?): List<XtreamLiveStreamDto> = error("network")
    override suspend fun vodCategories(): List<XtreamMediaCategoryDto> = error("network")
    override suspend fun vodStreams(categoryId: String?): List<XtreamVodStreamDto> = error("network")
    override suspend fun vodInfo(vodId: Int): XtreamVodInfoResponseDto = error("network")
    override suspend fun seriesCategories(): List<XtreamMediaCategoryDto> = error("network")
    override suspend fun series(categoryId: String?): List<XtreamSeriesDto> = error("network")
    override suspend fun seriesInfo(seriesId: Int): XtreamSeriesInfoResponseDto = error("network")
}
