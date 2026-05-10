package com.popcorn.live.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel

const val CATALOG_METADATA_ID = "catalog"

@Entity(tableName = "live_categories")
data class LiveCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
    val lastUpdatedAtMillis: Long,
)

@Entity(tableName = "live_channels")
data class LiveChannelEntity(
    @PrimaryKey val streamId: Int,
    val name: String,
    val categoryId: String,
    val streamIcon: String?,
    val streamType: String,
    val added: String?,
    val sortOrder: Int,
    val lastUpdatedAtMillis: Long,
)

@Entity(tableName = "catalog_metadata")
data class CatalogMetadataEntity(
    @PrimaryKey val id: String = CATALOG_METADATA_ID,
    val lastSuccessfulRefreshAt: Long?,
    val lastRefreshError: String?,
)

fun LiveCategory.toEntity(lastUpdatedAtMillis: Long) = LiveCategoryEntity(
    id = id,
    name = name,
    sortOrder = sortOrder,
    lastUpdatedAtMillis = lastUpdatedAtMillis,
)

fun LiveChannel.toEntity(lastUpdatedAtMillis: Long) = LiveChannelEntity(
    streamId = streamId,
    name = name,
    categoryId = categoryId,
    streamIcon = streamIcon,
    streamType = streamType,
    added = added,
    sortOrder = sortOrder,
    lastUpdatedAtMillis = lastUpdatedAtMillis,
)

fun LiveCategoryEntity.toDomain() = LiveCategory(
    id = id,
    name = name,
    sortOrder = sortOrder,
)

fun LiveChannelEntity.toDomain() = LiveChannel(
    streamId = streamId,
    name = name,
    categoryId = categoryId,
    streamIcon = streamIcon,
    streamType = streamType,
    added = added,
    sortOrder = sortOrder,
)

fun CatalogMetadataEntity.toDomain() = CatalogMetadata(
    lastSuccessfulRefreshAt = lastSuccessfulRefreshAt,
    lastRefreshError = lastRefreshError,
)
