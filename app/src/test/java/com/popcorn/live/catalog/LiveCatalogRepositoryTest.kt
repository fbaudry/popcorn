package com.popcorn.live.catalog

import com.popcorn.live.xtream.XtreamAccountResponseDto
import com.popcorn.live.xtream.XtreamApi
import com.popcorn.live.xtream.XtreamLiveCategoryDto
import com.popcorn.live.xtream.XtreamLiveStreamDto
import com.popcorn.live.xtream.XtreamMediaCategoryDto
import com.popcorn.live.xtream.XtreamSeriesDto
import com.popcorn.live.xtream.XtreamSeriesInfoResponseDto
import com.popcorn.live.xtream.XtreamVodInfoResponseDto
import com.popcorn.live.xtream.XtreamVodStreamDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCatalogRepositoryTest {
    @Test
    fun refreshStoresNormalizedCategoriesAndChannels() = runTest {
        val store = FakeLiveCatalogStore(
            initialCategories = listOf(LiveCategory("stale", "Stale", 0)),
            initialChannels = listOf(
                LiveChannel(1, "Stale Channel", "stale", null, "live", null, 0),
            ),
        )
        val repository = LiveCatalogRepository(
            api = SuccessfulXtreamApi(),
            store = store,
            clock = { 1234L },
        )

        val result = repository.refresh()

        assertEquals(RefreshResult.Success(refreshedAtMillis = 1234L), result)
        assertEquals(listOf(LiveCategory("10", "News", 0)), store.categories.value)
        assertEquals(
            listOf(LiveChannel(36475, "France 24", "10", null, "live", null, 0)),
            store.channels.value,
        )
        assertEquals(CatalogMetadata(lastSuccessfulRefreshAt = 1234L), store.metadata.value)
    }

    @Test
    fun refreshFailurePreservesExistingCache() = runTest {
        val store = FakeLiveCatalogStore(
            initialCategories = listOf(LiveCategory("cached", "Cached", 0)),
            initialChannels = listOf(
                LiveChannel(1, "Cached Channel", "cached", null, "live", null, 0),
            ),
            initialMetadata = CatalogMetadata(lastSuccessfulRefreshAt = 42L),
        )
        val repository = LiveCatalogRepository(
            api = FailingXtreamApi(),
            store = store,
        )

        val result = repository.refresh()

        assertTrue(result is RefreshResult.Failure)
        assertEquals("network", (result as RefreshResult.Failure).message)
        assertEquals(listOf(LiveCategory("cached", "Cached", 0)), store.categories.value)
        assertEquals(
            listOf(LiveChannel(1, "Cached Channel", "cached", null, "live", null, 0)),
            store.channels.value,
        )
        assertEquals(
            CatalogMetadata(lastSuccessfulRefreshAt = 42L, lastRefreshError = "network"),
            store.metadata.value,
        )
    }
}

private class SuccessfulXtreamApi : XtreamApi {
    override suspend fun account() = XtreamAccountResponseDto()

    override suspend fun liveCategories() = listOf(
        XtreamLiveCategoryDto(categoryId = "10", categoryName = " News "),
    )

    override suspend fun liveStreams(categoryId: String?) = listOf(
        XtreamLiveStreamDto(
            name = " France 24 ",
            streamId = 36475,
            categoryId = "10",
        ),
    )

    override suspend fun vodCategories(): List<XtreamMediaCategoryDto> = emptyList()
    override suspend fun vodStreams(categoryId: String?): List<XtreamVodStreamDto> = emptyList()
    override suspend fun vodInfo(vodId: Int): XtreamVodInfoResponseDto = XtreamVodInfoResponseDto()
    override suspend fun seriesCategories(): List<XtreamMediaCategoryDto> = emptyList()
    override suspend fun series(categoryId: String?): List<XtreamSeriesDto> = emptyList()
    override suspend fun seriesInfo(seriesId: Int): XtreamSeriesInfoResponseDto = XtreamSeriesInfoResponseDto()
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
