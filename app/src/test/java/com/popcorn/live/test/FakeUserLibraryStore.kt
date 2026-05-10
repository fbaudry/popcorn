package com.popcorn.live.test

import com.popcorn.live.user.LibraryItemKind
import com.popcorn.live.user.PlaybackProgress
import com.popcorn.live.user.UserLibraryRepository
import com.popcorn.live.user.UserLibraryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

fun fakeUserLibraryRepository(
    favorites: Map<LibraryItemKind, Set<String>> = emptyMap(),
    progress: Map<LibraryItemKind, List<PlaybackProgress>> = emptyMap(),
    lastPlayback: Map<LibraryItemKind, String?> = emptyMap(),
): UserLibraryRepository =
    UserLibraryRepository(
        FakeUserLibraryStore(
            favorites = favorites,
            progress = progress,
            lastPlayback = lastPlayback,
        ),
    )

private class FakeUserLibraryStore(
    favorites: Map<LibraryItemKind, Set<String>>,
    progress: Map<LibraryItemKind, List<PlaybackProgress>>,
    lastPlayback: Map<LibraryItemKind, String?>,
) : UserLibraryStore {
    private val favorites = MutableStateFlow(favorites)
    private val progress = MutableStateFlow(progress)
    private val lastPlayback = MutableStateFlow(lastPlayback)

    override fun favorites(kind: LibraryItemKind): Flow<Set<String>> =
        favorites.map { current -> current[kind].orEmpty() }

    override fun progress(kind: LibraryItemKind): Flow<List<PlaybackProgress>> =
        progress.map { current -> current[kind].orEmpty() }

    override fun lastPlayback(kind: LibraryItemKind): Flow<String?> =
        lastPlayback.map { current -> current[kind] }

    override suspend fun isFavorite(kind: LibraryItemKind, itemId: String): Boolean =
        favorites.value[kind]?.contains(itemId) == true

    override suspend fun setFavorite(kind: LibraryItemKind, itemId: String, favorite: Boolean) {
        val current = favorites.value
        val nextIds = if (favorite) {
            current[kind].orEmpty() + itemId
        } else {
            current[kind].orEmpty() - itemId
        }
        favorites.value = current + (kind to nextIds)
    }

    override suspend fun recordProgress(progress: PlaybackProgress) {
        val current = this.progress.value
        val next = current[progress.kind].orEmpty()
            .filterNot { item -> item.itemId == progress.itemId } + progress
        this.progress.value = current + (progress.kind to next)
    }

    override suspend fun recordLastPlayback(kind: LibraryItemKind, itemId: String) {
        lastPlayback.value = lastPlayback.value + (kind to itemId)
    }
}
