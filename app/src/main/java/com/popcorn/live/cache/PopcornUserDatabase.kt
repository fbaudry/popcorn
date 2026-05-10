package com.popcorn.live.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        PlaybackProgressEntity::class,
        LastPlaybackEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PopcornUserDatabase : RoomDatabase() {
    abstract fun userLibraryDao(): UserLibraryDao

    companion object {
        fun create(context: Context): PopcornUserDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                PopcornUserDatabase::class.java,
                "popcorn-user.db",
            ).build()
    }
}
