package com.popcorn.live.xtream

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

class XtreamNormalizerTest {
    @Test
    fun categoryNamesAreTrimmedAndSortedByApiOrder() {
        val categories = listOf(
            XtreamLiveCategoryDto(categoryId = "2", categoryName = " Sports ", parentId = 0),
            XtreamLiveCategoryDto(categoryId = "1", categoryName = "News", parentId = 0),
        )

        val normalized = XtreamNormalizer.categories(categories)

        assertEquals("2", normalized[0].id)
        assertEquals("Sports", normalized[0].name)
        assertEquals(0, normalized[0].sortOrder)
        assertEquals("1", normalized[1].id)
        assertEquals("News", normalized[1].name)
        assertEquals(1, normalized[1].sortOrder)
    }

    @Test
    fun liveChannelsKeepStreamIdCategoryAndIcon() {
        val channels = listOf(
            XtreamLiveStreamDto(
                num = 1,
                name = " France 24 ",
                streamType = "live",
                streamId = 36475,
                streamIcon = "https://example.com/france24.png",
                epgChannelId = "france24.fr",
                added = "1700000000",
                categoryId = "10",
                customSid = null,
                tvArchive = 0,
                directSource = "",
                tvArchiveDuration = JsonPrimitive(0),
            ),
        )

        val normalized = XtreamNormalizer.channels(channels)

        assertEquals(36475, normalized.single().streamId)
        assertEquals("France 24", normalized.single().name)
        assertEquals("10", normalized.single().categoryId)
        assertEquals("https://example.com/france24.png", normalized.single().streamIcon)
    }

    @Test
    fun liveChannelNamesKeepQualityWhenCategoryDoesNotContainQuality() {
        val channels = listOf(
            streamDto(streamId = 1, name = "|FR| Canal + live 1 FHD"),
            streamDto(streamId = 2, name = "[FR] TF1 SD"),
            streamDto(streamId = 3, name = "|FR| FRANCE 2 HD"),
            streamDto(streamId = 4, name = "M6 4K"),
            streamDto(streamId = 5, name = "ARTE"),
            streamDto(streamId = 6, name = "|FR| TF1 4K HDR UHD (Résolution: 3840x2160)"),
        )

        val names = XtreamNormalizer.channels(
            input = channels,
            categories = listOf(XtreamLiveCategoryDto(categoryId = "1", categoryName = "FR General")),
        ).map { it.name }

        assertEquals(
            listOf(
                "Canal + live 1 FHD",
                "TF1 SD",
                "FRANCE 2 HD",
                "M6 4K",
                "ARTE",
                "TF1 4K HDR UHD",
            ),
            names,
        )
    }

    @Test
    fun liveChannelNamesHideQualityWhenCategoryAlreadyContainsQuality() {
        val channels = listOf(
            streamDto(streamId = 1, name = "|FR| TF1 SD", categoryId = "sd"),
            streamDto(streamId = 2, name = "|FR| Canal + live 1 FHD", categoryId = "fhd"),
            streamDto(streamId = 3, name = "|FR| TF1 4K HDR UHD (Résolution: 3840x2160)", categoryId = "4k"),
            streamDto(streamId = 4, name = "|FR| M6 4K", categoryId = "mixed"),
        )

        val names = XtreamNormalizer.channels(
            input = channels,
            categories = listOf(
                XtreamLiveCategoryDto(categoryId = "sd", categoryName = "FR TV SD (FRANCE)"),
                XtreamLiveCategoryDto(categoryId = "fhd", categoryName = "FR TV FULL HD (France)"),
                XtreamLiveCategoryDto(categoryId = "4k", categoryName = "FR TV FULL HD|4K (France)"),
                XtreamLiveCategoryDto(categoryId = "mixed", categoryName = "FR TV 4K HDR"),
            ),
        ).map { it.name }

        assertEquals(
            listOf(
                "TF1",
                "Canal + live 1",
                "TF1",
                "M6",
            ),
            names,
        )
    }

    @Test
    fun liveStreamJsonAcceptsNullCategoryAndStringArchiveDuration() {
        val json = Json { ignoreUnknownKeys = true }
        val decoded = json.decodeFromString<List<XtreamLiveStreamDto>>(
            """
            [
              {
                "num": 12,
                "name": "Uncategorized",
                "stream_type": "live",
                "stream_id": 777,
                "stream_icon": "",
                "epg_channel_id": null,
                "added": "1539800514",
                "category_id": null,
                "custom_sid": null,
                "tv_archive": 1,
                "direct_source": "",
                "tv_archive_duration": "3"
              }
            ]
            """.trimIndent(),
        )

        val normalized = XtreamNormalizer.channels(decoded)

        assertEquals(777, normalized.single().streamId)
        assertEquals("", normalized.single().categoryId)
        assertEquals(null, normalized.single().streamIcon)
    }

    @Test
    fun moviesKeepPosterExtensionAndVariantRating() {
        val normalized = XtreamNormalizer.movies(
            listOf(
                XtreamVodStreamDto(
                    name = "|FR| The Movie FHD",
                    streamId = 42,
                    streamIcon = "https://example.com/movie.jpg",
                    rating = JsonPrimitive(4),
                    categoryId = JsonPrimitive("movies"),
                    containerExtension = "mkv",
                ),
            ),
        )

        assertEquals(42, normalized.single().id)
        assertEquals("The Movie FHD", normalized.single().name)
        assertEquals("https://example.com/movie.jpg", normalized.single().posterUrl)
        assertEquals("4", normalized.single().rating)
        assertEquals("movies", normalized.single().categoryId)
        assertEquals("mkv", normalized.single().containerExtension)
    }

    @Test
    fun seriesKeepCoverPlotAndVariantCategory() {
        val normalized = XtreamNormalizer.series(
            listOf(
                XtreamSeriesDto(
                    name = "[FR] Example Show",
                    seriesId = 99,
                    cover = "https://example.com/show.jpg",
                    plot = "A plot",
                    genre = "Drama",
                    rating = JsonPrimitive("8.2"),
                    releaseDate = "2024",
                    categoryId = JsonPrimitive(123),
                ),
            ),
        )

        assertEquals(99, normalized.single().id)
        assertEquals("Example Show", normalized.single().name)
        assertEquals("https://example.com/show.jpg", normalized.single().posterUrl)
        assertEquals("A plot", normalized.single().plot)
        assertEquals("Drama", normalized.single().genre)
        assertEquals("8.2", normalized.single().rating)
        assertEquals("2024", normalized.single().releaseDate)
        assertEquals("123", normalized.single().categoryId)
    }

    @Test
    fun seriesDetailsSortEpisodesAndSkipsMissingIds() {
        val details = XtreamNormalizer.seriesDetails(
            response = XtreamSeriesInfoResponseDto(
                info = XtreamSeriesInfoDto(
                    name = "|FR| Example Show",
                    cover = "https://example.com/show.jpg",
                    plot = "A plot",
                    rating = JsonPrimitive(7),
                ),
                episodes = mapOf(
                    "2" to listOf(
                        XtreamSeriesEpisodeDto(
                            id = JsonPrimitive("ep-3"),
                            title = "Third",
                            season = JsonPrimitive(2),
                            episodeNumber = JsonPrimitive(1),
                            containerExtension = "mkv",
                        ),
                    ),
                    "1" to listOf(
                        XtreamSeriesEpisodeDto(
                            id = JsonPrimitive("ep-2"),
                            title = "Second",
                            episodeNumber = JsonPrimitive(2),
                            containerExtension = "mp4",
                        ),
                        XtreamSeriesEpisodeDto(
                            id = null,
                            title = "Missing id",
                            episodeNumber = JsonPrimitive(3),
                        ),
                        XtreamSeriesEpisodeDto(
                            id = JsonPrimitive("ep-1"),
                            title = "First",
                            episodeNumber = JsonPrimitive(1),
                        ),
                    ),
                ),
            ),
        )

        assertEquals("Example Show", details.name)
        assertEquals("https://example.com/show.jpg", details.coverUrl)
        assertEquals("7", details.rating)
        assertEquals(listOf("ep-1", "ep-2", "ep-3"), details.episodes.map { it.id })
        assertEquals(listOf(1, 1, 2), details.episodes.map { it.season })
        assertEquals("mp4", details.episodes[0].containerExtension)
        assertEquals("mkv", details.episodes[2].containerExtension)
    }

    @Test
    fun mediaDtosParseObservedXtreamVariants() {
        val json = Json { ignoreUnknownKeys = true }
        val vod = json.decodeFromString<List<XtreamVodStreamDto>>(
            """
            [
              {
                "num": 1,
                "name": "Movie",
                "stream_type": "movie",
                "stream_id": 42,
                "stream_icon": "",
                "rating": "4.5",
                "rating_5based": 4,
                "category_id": null,
                "container_extension": "mp4"
              }
            ]
            """.trimIndent(),
        )
        val series = json.decodeFromString<List<XtreamSeriesDto>>(
            """
            [
              {
                "num": 1,
                "name": "Show",
                "series_id": 99,
                "cover": "",
                "rating_5based": "4.1",
                "backdrop_path": ["https://example.com/backdrop.jpg"],
                "category_id": "series"
              }
            ]
            """.trimIndent(),
        )
        val seriesInfo = json.decodeFromString<XtreamSeriesInfoResponseDto>(
            """
            {
              "info": {"name": "Show"},
              "episodes": {
                "1": [
                  {"id": "100", "episode_num": 1, "season": 1, "title": "Pilot", "info": {"duration": "45"}}
                ]
              },
              "seasons": []
            }
            """.trimIndent(),
        )

        assertEquals(42, vod.single().streamId)
        assertEquals(99, series.single().seriesId)
        assertEquals("100", seriesInfo.episodes.getValue("1").single().id?.let { id ->
            val primitive = id as JsonPrimitive
            primitive.content
        })
    }
}

private fun streamDto(
    streamId: Int,
    name: String,
    categoryId: String = "1",
) = XtreamLiveStreamDto(
    num = streamId,
    name = name,
    streamType = "live",
    streamId = streamId,
    categoryId = categoryId,
)
