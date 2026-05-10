package com.popcorn.live.user

import com.popcorn.live.catalog.MediaKind

enum class LibraryItemKind(val storageKey: String) {
    Live("live"),
    Movies("movies"),
    Series("series"),
}

fun MediaKind.toLibraryItemKind(): LibraryItemKind = when (this) {
    MediaKind.Movies -> LibraryItemKind.Movies
    MediaKind.Series -> LibraryItemKind.Series
}

data class PlaybackProgress(
    val kind: LibraryItemKind,
    val itemId: String,
    val parentId: String?,
    val title: String,
    val imageUrl: String?,
    val containerExtension: String?,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAtMillis: Long,
)
