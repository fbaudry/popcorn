package com.popcorn.live.cache

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserLibraryDaoTest {
    @Test
    fun storesFavoritesProgressAndLastPlayback() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PopcornUserDatabase::class.java,
        ).build()

        try {
            val dao = db.userLibraryDao()

            dao.upsertFavorite(
                FavoriteEntity(
                    kind = "movies",
                    itemId = "42",
                    createdAtMillis = 100L,
                ),
            )
            dao.upsertProgress(
                PlaybackProgressEntity(
                    kind = "movies",
                    itemId = "42",
                    parentId = null,
                    title = "Movie One",
                    imageUrl = "https://example.com/movie.jpg",
                    containerExtension = "mkv",
                    positionMillis = 60_000L,
                    durationMillis = 120_000L,
                    updatedAtMillis = 200L,
                ),
            )
            dao.upsertLastPlayback(
                LastPlaybackEntity(
                    kind = "live",
                    itemId = "36475",
                    updatedAtMillis = 300L,
                ),
            )

            assertEquals(listOf("42"), dao.observeFavorites("movies").first().map { row -> row.itemId })
            assertEquals("Movie One", dao.observeProgress("movies").first().single().title)
            assertEquals("36475", dao.observeLastPlayback("live").first()?.itemId)
        } finally {
            db.close()
        }
    }
}
