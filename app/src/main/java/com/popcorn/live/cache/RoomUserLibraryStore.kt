package com.popcorn.live.cache

import com.popcorn.live.user.LibraryItemKind
import com.popcorn.live.user.PlaybackProgress
import com.popcorn.live.user.UserLibraryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomUserLibraryStore(
    private val dao: UserLibraryDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : UserLibraryStore {
    override fun favorites(kind: LibraryItemKind): Flow<Set<String>> =
        dao.observeFavorites(kind.storageKey).map { favorites ->
            favorites.mapTo(linkedSetOf()) { favorite -> favorite.itemId }
        }

    override fun progress(kind: LibraryItemKind): Flow<List<PlaybackProgress>> =
        dao.observeProgress(kind.storageKey).map { progress ->
            progress.map(PlaybackProgressEntity::toDomain)
        }

    override fun lastPlayback(kind: LibraryItemKind): Flow<String?> =
        dao.observeLastPlayback(kind.storageKey).map { lastPlayback ->
            lastPlayback?.itemId
        }

    override suspend fun isFavorite(kind: LibraryItemKind, itemId: String): Boolean =
        dao.favorite(kind.storageKey, itemId) != null

    override suspend fun setFavorite(kind: LibraryItemKind, itemId: String, favorite: Boolean) {
        if (favorite) {
            dao.upsertFavorite(
                FavoriteEntity(
                    kind = kind.storageKey,
                    itemId = itemId,
                    createdAtMillis = clock(),
                ),
            )
        } else {
            dao.deleteFavorite(kind.storageKey, itemId)
        }
    }

    override suspend fun recordProgress(progress: PlaybackProgress) {
        if (shouldKeepProgress(progress)) {
            dao.upsertProgress(progress.toEntity())
        } else {
            dao.deleteProgress(progress.kind.storageKey, progress.itemId)
        }
    }

    override suspend fun recordLastPlayback(kind: LibraryItemKind, itemId: String) {
        dao.upsertLastPlayback(
            LastPlaybackEntity(
                kind = kind.storageKey,
                itemId = itemId,
                updatedAtMillis = clock(),
            ),
        )
    }

    private fun shouldKeepProgress(progress: PlaybackProgress): Boolean {
        if (progress.positionMillis < MIN_RESUME_POSITION_MILLIS) {
            return false
        }

        if (progress.durationMillis <= 0L) {
            return true
        }

        return progress.positionMillis < progress.durationMillis - COMPLETED_THRESHOLD_MILLIS
    }

    private companion object {
        const val MIN_RESUME_POSITION_MILLIS = 30_000L
        const val COMPLETED_THRESHOLD_MILLIS = 60_000L
    }
}
