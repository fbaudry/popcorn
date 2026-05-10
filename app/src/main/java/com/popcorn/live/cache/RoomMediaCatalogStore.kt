package com.popcorn.live.cache

import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.MediaCatalogStore
import com.popcorn.live.catalog.MediaCategory
import com.popcorn.live.catalog.MediaItem
import com.popcorn.live.catalog.MediaKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMediaCatalogStore(
    private val dao: MediaCatalogDao,
) : MediaCatalogStore {
    override fun categories(kind: MediaKind): Flow<List<MediaCategory>> =
        dao.observeCategories(kind.storageKey).map { rows -> rows.map(MediaCategoryEntity::toDomain) }

    override fun items(kind: MediaKind): Flow<List<MediaItem>> =
        dao.observeItems(kind.storageKey).map { rows -> rows.map(MediaItemEntity::toDomain) }

    override fun metadata(kind: MediaKind): Flow<CatalogMetadata> =
        dao.observeMetadata(kind.storageKey).map { row -> row?.toDomain() ?: CatalogMetadata() }

    override suspend fun replaceCatalog(
        kind: MediaKind,
        categories: List<MediaCategory>,
        items: List<MediaItem>,
        refreshedAtMillis: Long,
    ) {
        dao.replaceCatalog(
            kind = kind.storageKey,
            categories = categories.map { category -> category.toEntity(refreshedAtMillis) },
            items = items.map { item -> item.toEntity(refreshedAtMillis) },
            metadata = MediaMetadataEntity(
                kind = kind.storageKey,
                lastSuccessfulRefreshAt = refreshedAtMillis,
                lastRefreshError = null,
            ),
        )
    }

    override suspend fun recordRefreshError(kind: MediaKind, message: String) {
        val current = dao.currentMetadata(kind.storageKey)

        dao.upsertMetadata(
            MediaMetadataEntity(
                kind = kind.storageKey,
                lastSuccessfulRefreshAt = current?.lastSuccessfulRefreshAt,
                lastRefreshError = message,
            ),
        )
    }
}
