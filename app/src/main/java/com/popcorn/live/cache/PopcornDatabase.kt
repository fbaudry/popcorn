package com.popcorn.live.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LiveCategoryEntity::class,
        LiveChannelEntity::class,
        CatalogMetadataEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PopcornDatabase : RoomDatabase() {
    abstract fun liveCatalogDao(): LiveCatalogDao

    companion object {
        fun create(context: Context): PopcornDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                PopcornDatabase::class.java,
                "popcorn.db",
            ).build()
    }
}
