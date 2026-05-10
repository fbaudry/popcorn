package com.popcorn.live.cache

import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.LiveCatalogStore
import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomLiveCatalogStore(
    private val dao: LiveCatalogDao,
) : LiveCatalogStore {
    override val categories: Flow<List<LiveCategory>> =
        dao.observeCategories().map { rows -> rows.map(LiveCategoryEntity::toDomain) }

    override val channels: Flow<List<LiveChannel>> =
        dao.observeChannels().map { rows -> rows.map(LiveChannelEntity::toDomain) }

    override val metadata: Flow<CatalogMetadata> =
        dao.observeMetadata().map { row -> row?.toDomain() ?: CatalogMetadata() }

    override suspend fun replaceCatalog(
        categories: List<LiveCategory>,
        channels: List<LiveChannel>,
        refreshedAtMillis: Long,
    ) {
        dao.replaceCatalog(
            categories = categories.map { category -> category.toEntity(refreshedAtMillis) },
            channels = channels.map { channel -> channel.toEntity(refreshedAtMillis) },
            metadata = CatalogMetadataEntity(
                lastSuccessfulRefreshAt = refreshedAtMillis,
                lastRefreshError = null,
            ),
        )
    }

    override suspend fun recordRefreshError(message: String) {
        val current = dao.currentMetadata()

        dao.upsertMetadata(
            CatalogMetadataEntity(
                lastSuccessfulRefreshAt = current?.lastSuccessfulRefreshAt,
                lastRefreshError = message,
            ),
        )
    }
}
