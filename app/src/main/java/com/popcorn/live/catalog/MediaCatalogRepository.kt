package com.popcorn.live.catalog

import com.popcorn.live.xtream.XtreamApi
import com.popcorn.live.xtream.XtreamNormalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

interface MediaCatalogStore {
    fun categories(kind: MediaKind): Flow<List<MediaCategory>>
    fun items(kind: MediaKind): Flow<List<MediaItem>>
    fun metadata(kind: MediaKind): Flow<CatalogMetadata>

    suspend fun replaceCatalog(
        kind: MediaKind,
        categories: List<MediaCategory>,
        items: List<MediaItem>,
        refreshedAtMillis: Long,
    )

    suspend fun recordRefreshError(kind: MediaKind, message: String)
}

class MediaCatalogRepository(
    private val api: XtreamApi,
    private val store: MediaCatalogStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    fun categories(kind: MediaKind): Flow<List<MediaCategory>> = store.categories(kind)

    fun items(kind: MediaKind): Flow<List<MediaItem>> = store.items(kind)

    fun metadata(kind: MediaKind): Flow<CatalogMetadata> = store.metadata(kind)

    suspend fun refresh(kind: MediaKind): RefreshResult {
        return try {
            val categories = when (kind) {
                MediaKind.Movies -> XtreamNormalizer.mediaCategories(api.vodCategories(), kind)
                MediaKind.Series -> XtreamNormalizer.mediaCategories(api.seriesCategories(), kind)
            }
            val items = when (kind) {
                MediaKind.Movies -> XtreamNormalizer.movies(api.vodStreams())
                MediaKind.Series -> XtreamNormalizer.series(api.series())
            }
            val refreshedAtMillis = clock()

            store.replaceCatalog(
                kind = kind,
                categories = categories,
                items = items,
                refreshedAtMillis = refreshedAtMillis,
            )

            RefreshResult.Success(refreshedAtMillis)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            val message = throwable.message
                ?.takeIf(String::isNotBlank)
                ?: "Xtream media refresh failed"

            store.recordRefreshError(kind, message)
            RefreshResult.Failure(message)
        }
    }

    suspend fun seriesDetails(series: MediaItem): SeriesDetails {
        require(series.kind == MediaKind.Series) {
            "Series details can only be loaded for series media items."
        }

        return XtreamNormalizer.seriesDetails(
            response = api.seriesInfo(series.id),
            fallback = series,
        )
    }
}
