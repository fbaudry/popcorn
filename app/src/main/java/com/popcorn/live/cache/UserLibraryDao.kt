package com.popcorn.live.cache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserLibraryDao {
    @Query("SELECT * FROM favorites WHERE kind = :kind ORDER BY createdAtMillis DESC")
    fun observeFavorites(kind: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE kind = :kind AND itemId = :itemId LIMIT 1")
    suspend fun favorite(kind: String, itemId: String): FavoriteEntity?

    @Upsert
    suspend fun upsertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE kind = :kind AND itemId = :itemId")
    suspend fun deleteFavorite(kind: String, itemId: String)

    @Query("SELECT * FROM playback_progress WHERE kind = :kind ORDER BY updatedAtMillis DESC")
    fun observeProgress(kind: String): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE kind = :kind AND itemId = :itemId LIMIT 1")
    suspend fun progress(kind: String, itemId: String): PlaybackProgressEntity?

    @Upsert
    suspend fun upsertProgress(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE kind = :kind AND itemId = :itemId")
    suspend fun deleteProgress(kind: String, itemId: String)

    @Query("SELECT * FROM last_playback WHERE kind = :kind LIMIT 1")
    fun observeLastPlayback(kind: String): Flow<LastPlaybackEntity?>

    @Upsert
    suspend fun upsertLastPlayback(lastPlayback: LastPlaybackEntity)
}
