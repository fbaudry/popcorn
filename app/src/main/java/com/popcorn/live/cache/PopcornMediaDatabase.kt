package com.popcorn.live.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MediaCategoryEntity::class,
        MediaItemEntity::class,
        MediaMetadataEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PopcornMediaDatabase : RoomDatabase() {
    abstract fun mediaCatalogDao(): MediaCatalogDao

    companion object {
        fun create(context: Context): PopcornMediaDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                PopcornMediaDatabase::class.java,
                "popcorn-media.db",
            ).build()
    }
}
