package com.popcorn.live.xtream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

interface XtreamApi {
    suspend fun account(): XtreamAccountResponseDto
    suspend fun liveCategories(): List<XtreamLiveCategoryDto>
    suspend fun liveStreams(categoryId: String? = null): List<XtreamLiveStreamDto>
    suspend fun vodCategories(): List<XtreamMediaCategoryDto>
    suspend fun vodStreams(categoryId: String? = null): List<XtreamVodStreamDto>
    suspend fun vodInfo(vodId: Int): XtreamVodInfoResponseDto
    suspend fun seriesCategories(): List<XtreamMediaCategoryDto>
    suspend fun series(categoryId: String? = null): List<XtreamSeriesDto>
    suspend fun seriesInfo(seriesId: Int): XtreamSeriesInfoResponseDto
}

class OkHttpXtreamApi(
    private val urlFactory: XtreamUrlFactory,
    private val client: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : XtreamApi {
    override suspend fun account(): XtreamAccountResponseDto =
        getJson(urlFactory.accountInfoUrl())

    override suspend fun liveCategories(): List<XtreamLiveCategoryDto> =
        getJson(urlFactory.liveCategoriesUrl())

    override suspend fun liveStreams(categoryId: String?): List<XtreamLiveStreamDto> =
        getJson(urlFactory.liveStreamsUrl(categoryId))

    override suspend fun vodCategories(): List<XtreamMediaCategoryDto> =
        getJson(urlFactory.vodCategoriesUrl())

    override suspend fun vodStreams(categoryId: String?): List<XtreamVodStreamDto> =
        getJson(urlFactory.vodStreamsUrl(categoryId))

    override suspend fun vodInfo(vodId: Int): XtreamVodInfoResponseDto =
        getJson(urlFactory.vodInfoUrl(vodId))

    override suspend fun seriesCategories(): List<XtreamMediaCategoryDto> =
        getJson(urlFactory.seriesCategoriesUrl())

    override suspend fun series(categoryId: String?): List<XtreamSeriesDto> =
        getJson(urlFactory.seriesUrl(categoryId))

    override suspend fun seriesInfo(seriesId: Int): XtreamSeriesInfoResponseDto =
        getJson(urlFactory.seriesInfoUrl(seriesId))

    private suspend inline fun <reified T> getJson(url: String): T = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Xtream request failed with HTTP ${response.code}")
            }

            json.decodeFromString<T>(response.body.string())
        }
    }
}
