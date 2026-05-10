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
class LiveCatalogDaoTest {
    @Test
    fun replaceCatalogReplacesRowsAndStoresMetadata() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PopcornDatabase::class.java,
        ).build()

        try {
            val dao = db.liveCatalogDao()

            dao.replaceCatalog(
                categories = listOf(LiveCategoryEntity("old", "Old", 0, 100L)),
                channels = listOf(
                    LiveChannelEntity(
                        streamId = 1,
                        name = "Old Channel",
                        categoryId = "old",
                        streamIcon = null,
                        streamType = "live",
                        added = null,
                        sortOrder = 0,
                        lastUpdatedAtMillis = 100L,
                    ),
                ),
                metadata = CatalogMetadataEntity(
                    lastSuccessfulRefreshAt = 100L,
                    lastRefreshError = "old error",
                ),
            )

            dao.replaceCatalog(
                categories = listOf(LiveCategoryEntity("10", "News", 0, 200L)),
                channels = listOf(
                    LiveChannelEntity(
                        streamId = 36475,
                        name = "France 24",
                        categoryId = "10",
                        streamIcon = null,
                        streamType = "live",
                        added = null,
                        sortOrder = 0,
                        lastUpdatedAtMillis = 200L,
                    ),
                ),
                metadata = CatalogMetadataEntity(
                    lastSuccessfulRefreshAt = 200L,
                    lastRefreshError = null,
                ),
            )

            assertEquals(listOf("News"), dao.observeCategories().first().map { row -> row.name })
            assertEquals(listOf("France 24"), dao.observeChannels().first().map { row -> row.name })
            assertEquals(200L, dao.observeMetadata().first()?.lastSuccessfulRefreshAt)
            assertEquals(null, dao.observeMetadata().first()?.lastRefreshError)
        } finally {
            db.close()
        }
    }
}
