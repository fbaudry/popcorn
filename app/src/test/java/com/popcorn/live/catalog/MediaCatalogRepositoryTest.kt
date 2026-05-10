package com.popcorn.live.catalog

import com.popcorn.live.xtream.XtreamAccountResponseDto
import com.popcorn.live.xtream.XtreamApi
import com.popcorn.live.xtream.XtreamLiveCategoryDto
import com.popcorn.live.xtream.XtreamLiveStreamDto
import com.popcorn.live.xtream.XtreamMediaCategoryDto
import com.popcorn.live.xtream.XtreamSeriesDto
import com.popcorn.live.xtream.XtreamSeriesEpisodeDto
import com.popcorn.live.xtream.XtreamSeriesInfoDto
import com.popcorn.live.xtream.XtreamSeriesInfoResponseDto
import com.popcorn.live.xtream.XtreamVodInfoResponseDto
import com.popcorn.live.xtream.XtreamVodStreamDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class MediaCatalogRepositoryTest {
    @Test
    fun refreshMoviesStoresCategoriesAndItems() = runTest {
        val store = FakeMediaCatalogStore()
        val repository = MediaCatalogRepository(
            api = FakeMediaXtreamApi(
                vodCategories = listOf(XtreamMediaCategoryDto(categoryId = "10", categoryName = " Movies ")),
                vodStreams = listOf(
                    XtreamVodStreamDto(
                        name = "|FR| Movie One",
                        streamId = 42,
                        streamIcon = "https://example.com/movie.jpg",
                        categoryId = JsonPrimitive("10"),
                        containerExtension = "mkv",
                    ),
                ),
            ),
            store = store,
            clock = { 1234L },
        )

        val result = repository.refresh(MediaKind.Movies)

        assertEquals(RefreshResult.Success(refreshedAtMillis = 1234L), result)
        assertEquals(listOf(MediaCategory("10", "Movies", MediaKind.Movies, 0)), store.movieCategories.value)
        assertEquals(
            listOf(
                MediaItem(
                    id = 42,
                    kind = MediaKind.Movies,
                    name = "Movie One",
                    categoryId = "10",
                    posterUrl = "https://example.com/movie.jpg",
                    plot = null,
                    genre = null,
                    rating = null,
                    releaseDate = null,
                    containerExtension = "mkv",
                    sortOrder = 0,
                ),
            ),
            store.movieItems.value,
        )
        assertEquals(CatalogMetadata(lastSuccessfulRefreshAt = 1234L), store.movieMetadata.value)
    }

    @Test
    fun refreshSeriesStoresCategoriesAndItems() = runTest {
        val store = FakeMediaCatalogStore()
        val repository = MediaCatalogRepository(
            api = FakeMediaXtreamApi(
                seriesCategories = listOf(XtreamMediaCategoryDto(categoryId = "20", categoryName = " Shows ")),
                series = listOf(
                    XtreamSeriesDto(
                        name = "[FR] Show One",
                        seriesId = 99,
                        cover = "https://example.com/show.jpg",
                        plot = "A plot",
                        genre = "Drama",
                        categoryId = JsonPrimitive("20"),
                    ),
                ),
            ),
            store = store,
            clock = { 4321L },
        )

        val result = repository.refresh(MediaKind.Series)

        assertEquals(RefreshResult.Success(refreshedAtMillis = 4321L), result)
        assertEquals(listOf(MediaCategory("20", "Shows", MediaKind.Series, 0)), store.seriesCategories.value)
        assertEquals("Show One", store.seriesItems.value.single().name)
        assertEquals("A plot", store.seriesItems.value.single().plot)
        assertEquals(CatalogMetadata(lastSuccessfulRefreshAt = 4321L), store.seriesMetadata.value)
    }

    @Test
    fun loadsSeriesDetailsOnDemand() = runTest {
        val repository = MediaCatalogRepository(
            api = FakeMediaXtreamApi(
                seriesInfo = XtreamSeriesInfoResponseDto(
                    info = XtreamSeriesInfoDto(name = "Show One", cover = "https://example.com/show.jpg"),
                    episodes = mapOf(
                        "1" to listOf(
                            XtreamSeriesEpisodeDto(
                                id = JsonPrimitive("ep-1"),
                                title = "Pilot",
                                episodeNumber = JsonPrimitive(1),
                                containerExtension = "mp4",
                            ),
                        ),
                    ),
                ),
            ),
            store = FakeMediaCatalogStore(),
        )

        val details = repository.seriesDetails(
            MediaItem(
                id = 99,
                kind = MediaKind.Series,
                name = "Fallback",
                categoryId = "20",
                posterUrl = null,
                plot = null,
                genre = null,
                rating = null,
                releaseDate = null,
                containerExtension = null,
                sortOrder = 0,
            ),
        )

        assertEquals("Show One", details.name)
        assertEquals("ep-1", details.episodes.single().id)
        assertEquals("Pilot", details.episodes.single().title)
    }

    @Test
    fun refreshCancellationIsRethrownAndDoesNotPersistError() = runTest {
        val store = FakeMediaCatalogStore().apply {
            movieMetadata.value = CatalogMetadata(lastSuccessfulRefreshAt = 42L)
        }
        val repository = MediaCatalogRepository(
            api = CancellingMediaXtreamApi(),
            store = store,
        )

        try {
            repository.refresh(MediaKind.Movies)
            fail("Expected CancellationException")
        } catch (cancellation: CancellationException) {
            assertEquals("cancelled", cancellation.message)
        }

        assertEquals(CatalogMetadata(lastSuccessfulRefreshAt = 42L), store.movieMetadata.value)
    }
}

private class FakeMediaCatalogStore : MediaCatalogStore {
    val movieCategories = MutableStateFlow<List<MediaCategory>>(emptyList())
    val seriesCategories = MutableStateFlow<List<MediaCategory>>(emptyList())
    val movieItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val seriesItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val movieMetadata = MutableStateFlow(CatalogMetadata())
    val seriesMetadata = MutableStateFlow(CatalogMetadata())

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

private class FakeMediaXtreamApi(
    private val vodCategories: List<XtreamMediaCategoryDto> = emptyList(),
    private val vodStreams: List<XtreamVodStreamDto> = emptyList(),
    private val seriesCategories: List<XtreamMediaCategoryDto> = emptyList(),
    private val series: List<XtreamSeriesDto> = emptyList(),
    private val seriesInfo: XtreamSeriesInfoResponseDto = XtreamSeriesInfoResponseDto(),
) : XtreamApi {
    override suspend fun account(): XtreamAccountResponseDto = XtreamAccountResponseDto()
    override suspend fun liveCategories(): List<XtreamLiveCategoryDto> = emptyList()
    override suspend fun liveStreams(categoryId: String?): List<XtreamLiveStreamDto> = emptyList()
    override suspend fun vodCategories(): List<XtreamMediaCategoryDto> = vodCategories
    override suspend fun vodStreams(categoryId: String?): List<XtreamVodStreamDto> = vodStreams
    override suspend fun vodInfo(vodId: Int): XtreamVodInfoResponseDto = XtreamVodInfoResponseDto()
    override suspend fun seriesCategories(): List<XtreamMediaCategoryDto> = seriesCategories
    override suspend fun series(categoryId: String?): List<XtreamSeriesDto> = series
    override suspend fun seriesInfo(seriesId: Int): XtreamSeriesInfoResponseDto = seriesInfo
}

private class CancellingMediaXtreamApi : XtreamApi {
    override suspend fun account(): XtreamAccountResponseDto = XtreamAccountResponseDto()
    override suspend fun liveCategories(): List<XtreamLiveCategoryDto> = emptyList()
    override suspend fun liveStreams(categoryId: String?): List<XtreamLiveStreamDto> = emptyList()
    override suspend fun vodCategories(): List<XtreamMediaCategoryDto> = throw CancellationException("cancelled")
    override suspend fun vodStreams(categoryId: String?): List<XtreamVodStreamDto> = emptyList()
    override suspend fun vodInfo(vodId: Int): XtreamVodInfoResponseDto = XtreamVodInfoResponseDto()
    override suspend fun seriesCategories(): List<XtreamMediaCategoryDto> = emptyList()
    override suspend fun series(categoryId: String?): List<XtreamSeriesDto> = emptyList()
    override suspend fun seriesInfo(seriesId: Int): XtreamSeriesInfoResponseDto = XtreamSeriesInfoResponseDto()
}
