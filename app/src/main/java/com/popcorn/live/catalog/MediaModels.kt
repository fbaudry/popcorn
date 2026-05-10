package com.popcorn.live.catalog

enum class MediaKind(val storageKey: String) {
    Movies("movies"),
    Series("series"),
}

data class MediaCategory(
    val id: String,
    val name: String,
    val kind: MediaKind,
    val sortOrder: Int,
)

data class MediaItem(
    val id: Int,
    val kind: MediaKind,
    val name: String,
    val categoryId: String,
    val posterUrl: String?,
    val plot: String?,
    val genre: String?,
    val rating: String?,
    val releaseDate: String?,
    val containerExtension: String?,
    val sortOrder: Int,
)

data class SeriesDetails(
    val seriesId: Int,
    val name: String,
    val coverUrl: String?,
    val plot: String?,
    val genre: String?,
    val rating: String?,
    val releaseDate: String?,
    val episodes: List<SeriesEpisode>,
)

data class SeriesEpisode(
    val id: String,
    val title: String,
    val season: Int,
    val episodeNumber: Int,
    val containerExtension: String,
)
