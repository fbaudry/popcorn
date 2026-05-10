package com.popcorn.live.cache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaCatalogDao {
    @Query("SELECT * FROM media_categories WHERE kind = :kind ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeCategories(kind: String): Flow<List<MediaCategoryEntity>>

    @Query("SELECT * FROM media_items WHERE kind = :kind ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeItems(kind: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_metadata WHERE kind = :kind LIMIT 1")
    fun observeMetadata(kind: String): Flow<MediaMetadataEntity?>

    @Query("SELECT * FROM media_metadata WHERE kind = :kind LIMIT 1")
    suspend fun currentMetadata(kind: String): MediaMetadataEntity?

    @Upsert
    suspend fun upsertCategories(categories: List<MediaCategoryEntity>)

    @Upsert
    suspend fun upsertItems(items: List<MediaItemEntity>)

    @Upsert
    suspend fun upsertMetadata(metadata: MediaMetadataEntity)

    @Query("DELETE FROM media_items WHERE kind = :kind")
    suspend fun deleteItems(kind: String)

    @Query("DELETE FROM media_categories WHERE kind = :kind")
    suspend fun deleteCategories(kind: String)

    @Transaction
    suspend fun replaceCatalog(
        kind: String,
        categories: List<MediaCategoryEntity>,
        items: List<MediaItemEntity>,
        metadata: MediaMetadataEntity,
    ) {
        deleteItems(kind)
        deleteCategories(kind)
        upsertCategories(categories)
        upsertItems(items)
        upsertMetadata(metadata)
    }
}
