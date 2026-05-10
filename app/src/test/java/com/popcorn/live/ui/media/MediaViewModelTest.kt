package com.popcorn.live.ui.media

import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.MediaCatalogRepository
import com.popcorn.live.catalog.MediaCatalogStore
import com.popcorn.live.catalog.MediaCategory
import com.popcorn.live.catalog.MediaItem
import com.popcorn.live.catalog.MediaKind
import com.popcorn.live.test.fakeUserLibraryRepository
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewModelTest {
    @Test
    fun searchSuggestionsComeFromCachedMediaCatalog() = runTest {
        withMainDispatcher { dispatcher ->
            val viewModel = MoviesViewModel(
                repository = MediaCatalogRepository(
                    api = StaticMediaXtreamApi(
                        movies = listOf(
                            movieDto("Canal Movie", 1),
                            movieDto("Canal Premiere", 2),
                            movieDto("Another Film", 3),
                        ),
                    ),
                    store = FakeMediaStore(),
                ),
                userLibraryRepository = fakeUserLibraryRepository(
                    favorites = mapOf(LibraryItemKind.Movies to setOf("1", "2", "3")),
                ),
                dispatcher = dispatcher,
            )

            advanceUntilIdle()
            viewModel.onSearchChanged("canal")
            advanceUntilIdle()

            assertEquals(
                listOf("Canal Movie", "Canal Premiere"),
                viewModel.uiState.value.searchSuggestions,
            )
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

private class FakeMediaStore : MediaCatalogStore {
    private val movieCategories = MutableStateFlow<List<MediaCategory>>(emptyList())
    private val seriesCategories = MutableStateFlow<List<MediaCategory>>(emptyList())
    private val movieItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private val seriesItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private val movieMetadata = MutableStateFlow(CatalogMetadata())
    private val seriesMetadata = MutableStateFlow(CatalogMetadata())

    override fun categories(kind: MediaKind) = when (kind) {
        MediaKind.Movies -> movieCategories
        MediaKind.Series -> seriesCategories
    }

    override fun items(kind: MediaKind) = when (kind) {
        MediaKind.Movies -> movieItems
        MediaKind.Series -> seriesItems
    }

    override fun metadata(kind: MediaKind) = when (kind) {
        MediaKind.Movies -> movieMetadata
        MediaKind.Series -> seriesMetadata
    }

    override suspend fun replaceCatalog(
        kind: MediaKind,
        categories: List<MediaCategory>,
        items: List<MediaItem>,
        refreshedAtMillis: Long,
    ) {
        when (kind) {
            MediaKind.Movies -> {
                movieCategories.value = categories
                movieItems.value = items
                movieMetadata.value = CatalogMetadata(lastSuccessfulRefreshAt = refreshedAtMillis)
            }
            MediaKind.Series -> {
                seriesCategories.value = categories
                seriesItems.value = items
                seriesMetadata.value = CatalogMetadata(lastSuccessfulRefreshAt = refreshedAtMillis)
            }
        }
    }

    override suspend fun recordRefreshError(kind: MediaKind, message: String) {
        when (kind) {
            MediaKind.Movies -> movieMetadata.value = movieMetadata.value.copy(lastRefreshError = message)
            MediaKind.Series -> seriesMetadata.value = seriesMetadata.value.copy(lastRefreshError = message)
        }
    }
}

private class StaticMediaXtreamApi(
    private val movies: List<XtreamVodStreamDto> = emptyList(),
) : XtreamApi {
    override suspend fun account(): XtreamAccountResponseDto = XtreamAccountResponseDto()
    override suspend fun liveCategories(): List<XtreamLiveCategoryDto> = emptyList()
    override suspend fun liveStreams(categoryId: String?): List<XtreamLiveStreamDto> = emptyList()
    override suspend fun vodCategories(): List<XtreamMediaCategoryDto> =
        listOf(XtreamMediaCategoryDto(categoryId = "movies", categoryName = "Movies"))
    override suspend fun vodStreams(categoryId: String?): List<XtreamVodStreamDto> = movies
    override suspend fun vodInfo(vodId: Int): XtreamVodInfoResponseDto = XtreamVodInfoResponseDto()
    override suspend fun seriesCategories(): List<XtreamMediaCategoryDto> = emptyList()
    override suspend fun series(categoryId: String?): List<XtreamSeriesDto> = emptyList()
    override suspend fun seriesInfo(seriesId: Int): XtreamSeriesInfoResponseDto = XtreamSeriesInfoResponseDto()
}

private fun movieDto(name: String, streamId: Int) = XtreamVodStreamDto(
    name = name,
    streamId = streamId,
    categoryId = JsonPrimitive("movies"),
    containerExtension = "mp4",
)
