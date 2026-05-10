package com.popcorn.live.cache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveCatalogDao {
    @Query("SELECT * FROM live_categories ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeCategories(): Flow<List<LiveCategoryEntity>>

    @Query("SELECT * FROM live_channels ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeChannels(): Flow<List<LiveChannelEntity>>

    @Query("SELECT * FROM catalog_metadata WHERE id = 'catalog' LIMIT 1")
    fun observeMetadata(): Flow<CatalogMetadataEntity?>

    @Query("SELECT * FROM catalog_metadata WHERE id = 'catalog' LIMIT 1")
    suspend fun currentMetadata(): CatalogMetadataEntity?

    @Upsert
    suspend fun upsertCategories(categories: List<LiveCategoryEntity>)

    @Upsert
    suspend fun upsertChannels(channels: List<LiveChannelEntity>)

    @Upsert
    suspend fun upsertMetadata(metadata: CatalogMetadataEntity)

    @Query("DELETE FROM live_channels")
    suspend fun deleteChannels()

    @Query("DELETE FROM live_categories")
    suspend fun deleteCategories()

    @Transaction
    suspend fun replaceCatalog(
        categories: List<LiveCategoryEntity>,
        channels: List<LiveChannelEntity>,
        metadata: CatalogMetadataEntity,
    ) {
        deleteChannels()
        deleteCategories()
        upsertCategories(categories)
        upsertChannels(channels)
        upsertMetadata(metadata)
    }
}
