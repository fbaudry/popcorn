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
import com.popcorn.live.config.SharedPreferencesXtreamConnectionConfigRepository
import com.popcorn.live.config.XtreamConnectionConfig
import com.popcorn.live.config.XtreamConnectionConfigRepository
import com.popcorn.live.user.UserLibraryRepository
import com.popcorn.live.xtream.OkHttpXtreamApi
import com.popcorn.live.xtream.XtreamUrlFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class AppContainer(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val xtreamConnectionConfigRepository: XtreamConnectionConfigRepository =
        SharedPreferencesXtreamConnectionConfigRepository(context)

    private val database = PopcornDatabase.create(context)
    private val mediaDatabase = PopcornMediaDatabase.create(context)
    private val userDatabase = PopcornUserDatabase.create(context)
    private val httpClient = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var xtreamServices = xtreamConnectionConfigRepository.config.value
        ?.let(::createXtreamServices)

    val userLibraryRepository = UserLibraryRepository(
        store = RoomUserLibraryStore(userDatabase.userLibraryDao()),
    )

    suspend fun saveXtreamConnectionConfig(config: XtreamConnectionConfig) {
        val normalizedConfig = config.normalized()
        require(normalizedConfig.isValid) {
            "Xtream connection config must contain a base URL, username, and password."
        }
        val services = createXtreamServices(normalizedConfig)
        xtreamServices = services
        xtreamConnectionConfigRepository.save(normalizedConfig)
    }

    fun requireXtreamServices(): XtreamServices =
        xtreamServices ?: error("Xtream connection config is required before creating catalog services.")

    private fun createXtreamServices(config: XtreamConnectionConfig): XtreamServices {
        val urlFactory = XtreamUrlFactory(
            baseUrl = config.baseUrl,
            username = config.username,
            password = config.password,
        )
        val xtreamApi = OkHttpXtreamApi(
            urlFactory = urlFactory,
            client = httpClient,
            json = json,
            ioDispatcher = ioDispatcher,
        )

        return XtreamServices(
            liveCatalogRepository = LiveCatalogRepository(
                api = xtreamApi,
                store = RoomLiveCatalogStore(database.liveCatalogDao()),
            ),
            mediaCatalogRepository = MediaCatalogRepository(
                api = xtreamApi,
                store = RoomMediaCatalogStore(mediaDatabase.mediaCatalogDao()),
            ),
            xtreamUrlFactory = urlFactory,
        )
    }
}

data class XtreamServices(
    val liveCatalogRepository: LiveCatalogRepository,
    val mediaCatalogRepository: MediaCatalogRepository,
    val xtreamUrlFactory: XtreamUrlFactory,
)
