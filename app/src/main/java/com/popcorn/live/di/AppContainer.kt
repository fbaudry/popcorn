package com.popcorn.live.di

import android.content.Context
import com.popcorn.live.cache.PopcornDatabase
import com.popcorn.live.cache.PopcornMediaDatabase
import com.popcorn.live.cache.PopcornUserDatabase
import com.popcorn.live.cache.RoomLiveCatalogStore
import com.popcorn.live.cache.RoomMediaCatalogStore
import com.popcorn.live.cache.RoomUserLibraryStore
import com.popcorn.live.catalog.LiveCatalogRepository
import com.popcorn.live.catalog.MediaCatalogRepository
import com.popcorn.live.config.AppConfig
import com.popcorn.live.user.UserLibraryRepository
import com.popcorn.live.xtream.OkHttpXtreamApi
import com.popcorn.live.xtream.XtreamUrlFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    private val appConfig = AppConfig.fromBuildConfig()
    private val database = PopcornDatabase.create(context)
    private val mediaDatabase = PopcornMediaDatabase.create(context)
    private val userDatabase = PopcornUserDatabase.create(context)
    private val urlFactory = XtreamUrlFactory(
        baseUrl = appConfig.xtreamBaseUrl,
        username = appConfig.xtreamUsername,
        password = appConfig.xtreamPassword,
    )
    private val httpClient = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }
    private val xtreamApi = OkHttpXtreamApi(urlFactory, httpClient, json)

    val liveCatalogRepository = LiveCatalogRepository(
        api = xtreamApi,
        store = RoomLiveCatalogStore(database.liveCatalogDao()),
    )
    val mediaCatalogRepository = MediaCatalogRepository(
        api = xtreamApi,
        store = RoomMediaCatalogStore(mediaDatabase.mediaCatalogDao()),
    )
    val userLibraryRepository = UserLibraryRepository(
        store = RoomUserLibraryStore(userDatabase.userLibraryDao()),
    )
    val xtreamUrlFactory: XtreamUrlFactory = urlFactory
}
