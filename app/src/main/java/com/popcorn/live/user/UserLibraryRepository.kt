package com.popcorn.live.user

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface UserLibraryStore {
    fun favorites(kind: LibraryItemKind): Flow<Set<String>>
    fun progress(kind: LibraryItemKind): Flow<List<PlaybackProgress>>
    fun lastPlayback(kind: LibraryItemKind): Flow<String?>

    suspend fun isFavorite(kind: LibraryItemKind, itemId: String): Boolean
    suspend fun setFavorite(kind: LibraryItemKind, itemId: String, favorite: Boolean)
    suspend fun recordProgress(progress: PlaybackProgress)
    suspend fun recordLastPlayback(kind: LibraryItemKind, itemId: String)
}

class UserLibraryRepository(
    private val store: UserLibraryStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    fun favorites(kind: LibraryItemKind): Flow<Set<String>> = store.favorites(kind)

    fun progress(kind: LibraryItemKind): Flow<List<PlaybackProgress>> = store.progress(kind)

    fun progressById(kind: LibraryItemKind): Flow<Map<String, PlaybackProgress>> =
        progress(kind).map { progress ->
            progress.associateBy { item -> item.itemId }
        }

    fun lastPlayback(kind: LibraryItemKind): Flow<String?> = store.lastPlayback(kind)

    suspend fun toggleFavorite(kind: LibraryItemKind, itemId: String) {
        val nextFavorite = !store.isFavorite(kind, itemId)
        store.setFavorite(kind, itemId, nextFavorite)
    }

    suspend fun recordProgress(
        kind: LibraryItemKind,
        itemId: String,
        parentId: String?,
        title: String,
        imageUrl: String?,
        containerExtension: String?,
        positionMillis: Long,
        durationMillis: Long,
    ) {
        if (kind == LibraryItemKind.Live) {
            return
        }

        store.recordProgress(
            PlaybackProgress(
                kind = kind,
                itemId = itemId,
                parentId = parentId,
                title = title,
                imageUrl = imageUrl,
                containerExtension = containerExtension,
                positionMillis = positionMillis,
                durationMillis = durationMillis,
                updatedAtMillis = clock(),
            ),
        )
    }

    suspend fun recordLastLiveChannel(streamId: Int) {
        store.recordLastPlayback(
            kind = LibraryItemKind.Live,
            itemId = streamId.toString(),
        )
    }
}
