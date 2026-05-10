package com.popcorn.live.cache

import androidx.room.Entity
import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.MediaCategory
import com.popcorn.live.catalog.MediaItem
import com.popcorn.live.catalog.MediaKind

@Entity(tableName = "media_categories", primaryKeys = ["kind", "id"])
data class MediaCategoryEntity(
    val kind: String,
    val id: String,
    val name: String,
    val sortOrder: Int,
    val lastUpdatedAtMillis: Long,
)

@Entity(tableName = "media_items", primaryKeys = ["kind", "id"])
data class MediaItemEntity(
    val kind: String,
    val id: Int,
    val name: String,
    val categoryId: String,
    val posterUrl: String?,
    val plot: String?,
    val genre: String?,
    val rating: String?,
    val releaseDate: String?,
    val containerExtension: String?,
    val sortOrder: Int,
    val lastUpdatedAtMillis: Long,
)

@Entity(tableName = "media_metadata", primaryKeys = ["kind"])
data class MediaMetadataEntity(
    val kind: String,
    val lastSuccessfulRefreshAt: Long?,
    val lastRefreshError: String?,
)

fun MediaCategory.toEntity(lastUpdatedAtMillis: Long) = MediaCategoryEntity(
    kind = kind.storageKey,
    id = id,
    name = name,
    sortOrder = sortOrder,
    lastUpdatedAtMillis = lastUpdatedAtMillis,
)

fun MediaItem.toEntity(lastUpdatedAtMillis: Long) = MediaItemEntity(
    kind = kind.storageKey,
    id = id,
    name = name,
    categoryId = categoryId,
    posterUrl = posterUrl,
    plot = plot,
    genre = genre,
    rating = rating,
    releaseDate = releaseDate,
    containerExtension = containerExtension,
    sortOrder = sortOrder,
    lastUpdatedAtMillis = lastUpdatedAtMillis,
)

fun MediaCategoryEntity.toDomain() = MediaCategory(
    id = id,
    name = name,
    kind = kind.toMediaKind(),
    sortOrder = sortOrder,
)

fun MediaItemEntity.toDomain() = MediaItem(
    id = id,
    kind = kind.toMediaKind(),
    name = name,
    categoryId = categoryId,
    posterUrl = posterUrl,
    plot = plot,
    genre = genre,
    rating = rating,
    releaseDate = releaseDate,
    containerExtension = containerExtension,
    sortOrder = sortOrder,
)

fun MediaMetadataEntity.toDomain() = CatalogMetadata(
    lastSuccessfulRefreshAt = lastSuccessfulRefreshAt,
    lastRefreshError = lastRefreshError,
)

private fun String.toMediaKind(): MediaKind =
    MediaKind.entries.firstOrNull { kind -> kind.storageKey == this } ?: MediaKind.Movies
