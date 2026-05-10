package com.popcorn.live.xtream

import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel
import com.popcorn.live.catalog.MediaCategory
import com.popcorn.live.catalog.MediaItem
import com.popcorn.live.catalog.MediaKind
import com.popcorn.live.catalog.SeriesDetails
import com.popcorn.live.catalog.SeriesEpisode
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

object XtreamNormalizer {
    fun categories(input: List<XtreamLiveCategoryDto>): List<LiveCategory> =
        input.mapIndexed { index, dto ->
            LiveCategory(
                id = dto.categoryId.orEmpty(),
                name = dto.categoryName.trim(),
                sortOrder = index,
            )
        }

    fun channels(
        input: List<XtreamLiveStreamDto>,
        categories: List<XtreamLiveCategoryDto> = emptyList(),
    ): List<LiveChannel> {
        val categoryIdsWithQuality = categories
            .filter { category -> containsVideoQuality(category.categoryName) }
            .map { category -> category.categoryId.orEmpty() }
            .toSet()

        return input.mapIndexed { index, dto ->
            val categoryId = dto.categoryId.orEmpty()
            val categoryContainsVideoQuality = categoryId in categoryIdsWithQuality

            LiveChannel(
                streamId = dto.streamId,
                name = cleanChannelName(
                    rawName = dto.name,
                    removeVideoQuality = categoryContainsVideoQuality,
                ),
                categoryId = categoryId,
                streamIcon = dto.streamIcon?.takeIf { it.isNotBlank() },
                streamType = dto.streamType,
                added = dto.added,
                sortOrder = index,
            )
        }
    }

    fun mediaCategories(
        input: List<XtreamMediaCategoryDto>,
        kind: MediaKind,
    ): List<MediaCategory> =
        input.mapIndexed { index, dto ->
            MediaCategory(
                id = dto.categoryId.orEmpty(),
                name = dto.categoryName.trim(),
                kind = kind,
                sortOrder = index,
            )
        }

    fun movies(input: List<XtreamVodStreamDto>): List<MediaItem> =
        input.mapIndexed { index, dto ->
            MediaItem(
                id = dto.streamId,
                kind = MediaKind.Movies,
                name = cleanMediaTitle(dto.name),
                categoryId = dto.categoryId.asCleanString(),
                posterUrl = dto.streamIcon?.takeIf { it.isNotBlank() },
                plot = null,
                genre = null,
                rating = dto.rating.asCleanString().takeIf(String::isNotBlank),
                releaseDate = null,
                containerExtension = dto.containerExtension.cleanMediaExtension(),
                sortOrder = index,
            )
        }

    fun series(input: List<XtreamSeriesDto>): List<MediaItem> =
        input.mapIndexed { index, dto ->
            MediaItem(
                id = dto.seriesId,
                kind = MediaKind.Series,
                name = cleanMediaTitle(dto.name),
                categoryId = dto.categoryId.asCleanString(),
                posterUrl = dto.cover?.takeIf { it.isNotBlank() },
                plot = dto.plot?.takeIf { it.isNotBlank() },
                genre = dto.genre?.takeIf { it.isNotBlank() },
                rating = dto.rating.asCleanString().takeIf(String::isNotBlank),
                releaseDate = dto.releaseDate?.takeIf { it.isNotBlank() }
                    ?: dto.releaseDateAlt?.takeIf { it.isNotBlank() },
                containerExtension = null,
                sortOrder = index,
            )
        }

    fun seriesDetails(
        response: XtreamSeriesInfoResponseDto,
        fallback: MediaItem? = null,
    ): SeriesDetails {
        val info = response.info
        val episodes = response.episodes
            .flatMap { (seasonKey, episodeDtos) ->
                episodeDtos.mapIndexedNotNull { index, dto ->
                    val id = dto.id.asCleanString().takeIf(String::isNotBlank) ?: return@mapIndexedNotNull null
                    val season = dto.season.asInt()
                        ?: seasonKey.toIntOrNull()
                        ?: 0
                    val episodeNumber = dto.episodeNumber.asInt() ?: index + 1
                    SeriesEpisode(
                        id = id,
                        title = dto.title
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                            ?: "Episode $episodeNumber",
                        season = season,
                        episodeNumber = episodeNumber,
                        containerExtension = dto.containerExtension.cleanMediaExtension(),
                    )
                }
            }
            .sortedWith(compareBy<SeriesEpisode> { it.season }.thenBy { it.episodeNumber })

        return SeriesDetails(
            seriesId = fallback?.id ?: 0,
            name = info?.name?.let(::cleanMediaTitle)
                ?: fallback?.name
                ?: "Série",
            coverUrl = info?.cover?.takeIf { it.isNotBlank() } ?: fallback?.posterUrl,
            plot = info?.plot?.takeIf { it.isNotBlank() } ?: fallback?.plot,
            genre = info?.genre?.takeIf { it.isNotBlank() } ?: fallback?.genre,
            rating = info?.rating.asCleanString().takeIf(String::isNotBlank) ?: fallback?.rating,
            releaseDate = info?.releaseDate?.takeIf { it.isNotBlank() }
                ?: info?.releaseDateAlt?.takeIf { it.isNotBlank() }
                ?: fallback?.releaseDate,
            episodes = episodes,
        )
    }

    fun cleanChannelName(
        rawName: String,
        removeVideoQuality: Boolean = false,
    ): String {
        val original = rawName.trim()
        val withoutLanguageAndDetails = original
            .replace(leadingLanguageTags, "")
            .replace(trailingTechnicalParentheses, "")
        val cleaned = if (removeVideoQuality) {
            withoutLanguageAndDetails.replace(trailingQualityTags, "")
        } else {
            withoutLanguageAndDetails
        }

        val normalized = cleaned
            .replace(repeatedSpaces, " ")
            .trim()
            .trim('|', '-', ':', '/', ' ')

        return normalized.ifBlank { original }
    }

    fun cleanMediaTitle(rawName: String): String =
        cleanChannelName(rawName = rawName, removeVideoQuality = false)

    private fun containsVideoQuality(value: String): Boolean =
        videoQualityToken.containsMatchIn(value)

    private val videoQualityToken = Regex(
        pattern = """(?:^|[^A-Z0-9])(?:SD|HD|FHD|FULL\s*HD|UHD|4K|HDR|HEVC|H\.?265|H\.?264|2160P|1080P|720P|50FPS|60FPS)(?:$|[^A-Z0-9])""",
        option = RegexOption.IGNORE_CASE,
    )

    private val leadingLanguageTags = Regex(
        pattern = """^(?:(?:\|\s*(?:FR|FRA|FRENCH|VF|VOSTFR)\s*\|)|(?:[\[\(\{]\s*(?:FR|FRA|FRENCH|VF|VOSTFR)\s*[\]\)\}])|(?:(?:FR|FRA|FRENCH|VF|VOSTFR)\s*[\|\-:/]+))\s*""",
        option = RegexOption.IGNORE_CASE,
    )

    private val trailingQualityTags = Regex(
        pattern = """(?:\s*[\|\-_/]*\s*(?:SD|HD|FHD|FULL\s*HD|UHD|4K|HDR|HEVC|H\.?265|H\.?264|2160P|1080P|720P|50FPS|60FPS))+$""",
        option = RegexOption.IGNORE_CASE,
    )

    private val trailingTechnicalParentheses = Regex(
        pattern = """\s*\([^)]*(?:SD|HD|FHD|FULL\s*HD|UHD|4K|HDR|RESOLUTION|RÉSOLUTION|DOLBY|HEVC|H\.?265|H\.?264|2160P|1080P|720P|50FPS|60FPS)[^)]*\)\s*$""",
        option = RegexOption.IGNORE_CASE,
    )

    private val repeatedSpaces = Regex("""\s{2,}""")

    private fun String?.cleanMediaExtension(): String =
        this
            ?.trim()
            ?.trimStart('.')
            ?.takeIf(String::isNotBlank)
            ?: DEFAULT_MEDIA_EXTENSION

    private fun JsonElement?.asCleanString(): String =
        when (this) {
            is JsonPrimitive -> contentOrNull ?: toString()
            null -> ""
            else -> toString()
        }.trim().trim('"')

    private fun JsonElement?.asInt(): Int? =
        when (this) {
            is JsonPrimitive -> intOrNull ?: contentOrNull?.toIntOrNull()
            else -> null
        }

    private const val DEFAULT_MEDIA_EXTENSION = "mp4"
}
