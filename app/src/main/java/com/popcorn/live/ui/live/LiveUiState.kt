package com.popcorn.live.ui.live

import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel
import com.popcorn.live.ui.navigation.CatalogMenuIds
import com.popcorn.live.ui.navigation.CatalogMenuItem

data class LiveUiState(
    val categories: List<LiveCategory> = emptyList(),
    val menuItems: List<CatalogMenuItem> = emptyList(),
    val visibleChannels: List<LiveChannel> = emptyList(),
    val favoriteChannelIds: Set<Int> = emptySet(),
    val lastLiveChannelId: Int? = null,
    val selectedMenuItemId: String = CatalogMenuIds.FAVORITES,
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val searchSuggestions: List<String> = emptyList(),
    val metadata: CatalogMetadata = CatalogMetadata(),
    val isRefreshing: Boolean = false,
    val blockingError: String? = null,
)
