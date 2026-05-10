package com.popcorn.live.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class XtreamAccountResponseDto(
    @SerialName("user_info") val userInfo: XtreamUserInfoDto? = null,
)

@Serializable
data class XtreamUserInfoDto(
    val auth: Int = 0,
    val status: String = "",
    val username: String = "",
)

@Serializable
data class XtreamLiveCategoryDto(
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String,
    @SerialName("parent_id") val parentId: Int? = null,
)

@Serializable
data class XtreamLiveStreamDto(
    val num: Int? = null,
    val name: String,
    @SerialName("stream_type") val streamType: String = "live",
    @SerialName("stream_id") val streamId: Int,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    val added: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("tv_archive") val tvArchive: Int? = null,
    @SerialName("direct_source") val directSource: String? = null,
    @SerialName("tv_archive_duration") val tvArchiveDuration: JsonElement? = null,
)

@Serializable
data class XtreamMediaCategoryDto(
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String,
    @SerialName("parent_id") val parentId: Int? = null,
)

@Serializable
data class XtreamVodStreamDto(
    val num: Int? = null,
    val name: String,
    @SerialName("stream_type") val streamType: String = "movie",
    @SerialName("stream_id") val streamId: Int,
    @SerialName("stream_icon") val streamIcon: String? = null,
    val rating: JsonElement? = null,
    val added: JsonElement? = null,
    @SerialName("category_id") val categoryId: JsonElement? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("direct_source") val directSource: String? = null,
)

@Serializable
data class XtreamVodInfoResponseDto(
    val info: XtreamVodInfoDto? = null,
    @SerialName("movie_data") val movieData: XtreamMovieDataDto? = null,
)

@Serializable
data class XtreamVodInfoDto(
    val name: String? = null,
    @SerialName("o_name") val originalName: String? = null,
    val plot: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val rating: JsonElement? = null,
    @SerialName("releasedate") val releaseDate: String? = null,
    @SerialName("movie_image") val movieImage: String? = null,
    @SerialName("cover_big") val coverBig: String? = null,
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
)

@Serializable
data class XtreamMovieDataDto(
    @SerialName("stream_id") val streamId: Int? = null,
    val name: String? = null,
    val added: JsonElement? = null,
    @SerialName("category_id") val categoryId: JsonElement? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("direct_source") val directSource: String? = null,
)

@Serializable
data class XtreamSeriesDto(
    val num: Int? = null,
    val name: String,
    @SerialName("series_id") val seriesId: Int,
    val cover: String? = null,
    val plot: String? = null,
    val genre: String? = null,
    val rating: JsonElement? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("release_date") val releaseDateAlt: String? = null,
    @SerialName("last_modified") val lastModified: JsonElement? = null,
    @SerialName("category_id") val categoryId: JsonElement? = null,
)

@Serializable
data class XtreamSeriesInfoResponseDto(
    val episodes: Map<String, List<XtreamSeriesEpisodeDto>> = emptyMap(),
    val info: XtreamSeriesInfoDto? = null,
    val seasons: List<XtreamSeriesSeasonDto> = emptyList(),
)

@Serializable
data class XtreamSeriesInfoDto(
    val name: String? = null,
    val cover: String? = null,
    val plot: String? = null,
    val genre: String? = null,
    val rating: JsonElement? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("release_date") val releaseDateAlt: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: JsonElement? = null,
)

@Serializable
data class XtreamSeriesSeasonDto(
    val id: JsonElement? = null,
    val name: String? = null,
    @SerialName("season_number") val seasonNumber: JsonElement? = null,
    @SerialName("episode_count") val episodeCount: JsonElement? = null,
)

@Serializable
data class XtreamSeriesEpisodeDto(
    val id: JsonElement? = null,
    @SerialName("episode_num") val episodeNumber: JsonElement? = null,
    val title: String? = null,
    val season: JsonElement? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("direct_source") val directSource: String? = null,
    val info: JsonElement? = null,
)
