package com.popcorn.live.catalog

import com.popcorn.live.xtream.XtreamApi
import com.popcorn.live.xtream.XtreamNormalizer
import kotlinx.coroutines.flow.Flow

data class CatalogMetadata(
    val lastSuccessfulRefreshAt: Long? = null,
    val lastRefreshError: String? = null,
)

sealed interface RefreshResult {
    data class Success(val refreshedAtMillis: Long) : RefreshResult
    data class Failure(val message: String) : RefreshResult
}

interface LiveCatalogStore {
    val categories: Flow<List<LiveCategory>>
    val channels: Flow<List<LiveChannel>>
    val metadata: Flow<CatalogMetadata>

    suspend fun replaceCatalog(
        categories: List<LiveCategory>,
        channels: List<LiveChannel>,
        refreshedAtMillis: Long,
    )

    suspend fun recordRefreshError(message: String)
}

class LiveCatalogRepository(
    private val api: XtreamApi,
    private val store: LiveCatalogStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    val categories: Flow<List<LiveCategory>> = store.categories
    val channels: Flow<List<LiveChannel>> = store.channels
    val metadata: Flow<CatalogMetadata> = store.metadata

    suspend fun refresh(): RefreshResult {
        return runCatching {
            val liveCategories = api.liveCategories()
            val categories = XtreamNormalizer.categories(liveCategories)
            val channels = XtreamNormalizer.channels(
                input = api.liveStreams(),
                categories = liveCategories,
            )
            val refreshedAtMillis = clock()

            store.replaceCatalog(
                categories = categories,
                channels = channels,
                refreshedAtMillis = refreshedAtMillis,
            )

            RefreshResult.Success(refreshedAtMillis)
        }.getOrElse { throwable ->
            val message = throwable.message
                ?.takeIf(String::isNotBlank)
                ?: "Xtream refresh failed"

            store.recordRefreshError(message)
            RefreshResult.Failure(message)
        }
    }
}
