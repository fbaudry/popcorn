package com.popcorn.live.cache

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationBaselineTest {
    @get:Rule
    val liveHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PopcornDatabase::class.java,
    )

    @get:Rule
    val mediaHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PopcornMediaDatabase::class.java,
    )

    @get:Rule
    val userHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PopcornUserDatabase::class.java,
    )

    @Test
    fun liveCatalogSchemaVersionOneOpensAsLatest() {
        liveHelper.createDatabase(LIVE_DATABASE_NAME, 1).close()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PopcornDatabase::class.java,
            LIVE_DATABASE_NAME,
        ).build()
        try {
            database.openHelper.writableDatabase
        } finally {
            database.close()
        }
    }

    @Test
    fun mediaCatalogSchemaVersionOneOpensAsLatest() {
        mediaHelper.createDatabase(MEDIA_DATABASE_NAME, 1).close()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PopcornMediaDatabase::class.java,
            MEDIA_DATABASE_NAME,
        ).build()
        try {
            database.openHelper.writableDatabase
        } finally {
            database.close()
        }
    }

    @Test
    fun userLibrarySchemaVersionOneOpensAsLatest() {
        userHelper.createDatabase(USER_DATABASE_NAME, 1).close()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PopcornUserDatabase::class.java,
            USER_DATABASE_NAME,
        ).build()
        try {
            database.openHelper.writableDatabase
        } finally {
            database.close()
        }
    }

    private companion object {
        const val LIVE_DATABASE_NAME = "migration-live.db"
        const val MEDIA_DATABASE_NAME = "migration-media.db"
        const val USER_DATABASE_NAME = "migration-user.db"
    }
}
