package com.popcorn.live.cache

import androidx.room.Entity
import com.popcorn.live.user.LibraryItemKind
import com.popcorn.live.user.PlaybackProgress

@Entity(tableName = "favorites", primaryKeys = ["kind", "itemId"])
data class FavoriteEntity(
    val kind: String,
    val itemId: String,
    val createdAtMillis: Long,
)

@Entity(tableName = "playback_progress", primaryKeys = ["kind", "itemId"])
data class PlaybackProgressEntity(
    val kind: String,
    val itemId: String,
    val parentId: String?,
    val title: String,
    val imageUrl: String?,
    val containerExtension: String?,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(tableName = "last_playback", primaryKeys = ["kind"])
data class LastPlaybackEntity(
    val kind: String,
    val itemId: String,
    val updatedAtMillis: Long,
)

fun PlaybackProgress.toEntity() = PlaybackProgressEntity(
    kind = kind.storageKey,
    itemId = itemId,
    parentId = parentId,
    title = title,
    imageUrl = imageUrl,
    containerExtension = containerExtension,
    positionMillis = positionMillis,
    durationMillis = durationMillis,
    updatedAtMillis = updatedAtMillis,
)

fun PlaybackProgressEntity.toDomain() = PlaybackProgress(
    kind = kind.toLibraryItemKind(),
    itemId = itemId,
    parentId = parentId,
    title = title,
    imageUrl = imageUrl,
    containerExtension = containerExtension,
    positionMillis = positionMillis,
    durationMillis = durationMillis,
    updatedAtMillis = updatedAtMillis,
)

fun String.toLibraryItemKind(): LibraryItemKind =
    LibraryItemKind.entries.firstOrNull { itemKind -> itemKind.storageKey == this }
        ?: LibraryItemKind.Live
