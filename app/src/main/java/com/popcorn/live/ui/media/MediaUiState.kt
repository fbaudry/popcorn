package com.popcorn.live.ui.media

import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.MediaCategory
import com.popcorn.live.catalog.MediaItem
import com.popcorn.live.catalog.SeriesDetails
import com.popcorn.live.ui.navigation.CatalogMenuIds
import com.popcorn.live.ui.navigation.CatalogMenuItem
import com.popcorn.live.user.PlaybackProgress

data class MediaUiState(
    val categories: List<MediaCategory> = emptyList(),
    val menuItems: List<CatalogMenuItem> = emptyList(),
    val visibleItems: List<MediaItem> = emptyList(),
    val favoriteItemIds: Set<Int> = emptySet(),
    val progressByItemId: Map<String, PlaybackProgress> = emptyMap(),
    val resumeItems: List<PlaybackProgress> = emptyList(),
    val selectedMenuItemId: String = CatalogMenuIds.FAVORITES,
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val searchSuggestions: List<String> = emptyList(),
    val metadata: CatalogMetadata = CatalogMetadata(),
    val isRefreshing: Boolean = false,
    val blockingError: String? = null,
    val selectedSeries: MediaItem? = null,
    val seriesDetails: SeriesDetails? = null,
    val isLoadingDetails: Boolean = false,
    val detailsError: String? = null,
)
