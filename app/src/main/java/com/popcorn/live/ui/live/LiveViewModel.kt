package com.popcorn.live.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.LiveCatalogRepository
import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel
import com.popcorn.live.ui.navigation.CatalogMenuIds
import com.popcorn.live.ui.navigation.CatalogMenuItem
import com.popcorn.live.ui.navigation.CatalogMenuItemType
import com.popcorn.live.user.LibraryItemKind
import com.popcorn.live.user.UserLibraryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LiveViewModel(
    private val repository: LiveCatalogRepository,
    private val userLibraryRepository: UserLibraryRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val selectedMenuItemId = MutableStateFlow(CatalogMenuIds.FAVORITES)
    private val searchQuery = MutableStateFlow("")
    private val isRefreshing = MutableStateFlow(false)
    private val controls = combine(
        selectedMenuItemId,
        searchQuery,
        isRefreshing,
    ) { selectedMenuItemId, searchQuery, isRefreshing ->
        LiveControls(
            selectedMenuItemId = selectedMenuItemId,
            searchQuery = searchQuery,
            isRefreshing = isRefreshing,
        )
    }

    val uiState = combine(
        repository.categories,
        repository.channels,
        repository.metadata,
        userLibraryRepository.favorites(LibraryItemKind.Live),
        userLibraryRepository.lastPlayback(LibraryItemKind.Live),
        controls,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        buildUiState(
            categories = values[0] as List<LiveCategory>,
            channels = values[1] as List<LiveChannel>,
            metadata = values[2] as CatalogMetadata,
            favoriteIds = values[3] as Set<String>,
            lastPlaybackId = values[4] as String?,
            controls = values[5] as LiveControls,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LiveUiState())

    init {
        refresh(silent = true)
    }

    fun onMenuItemSelected(menuItemId: String) {
        selectedMenuItemId.value = menuItemId
    }

    fun selectFavorites() {
        selectedMenuItemId.value = CatalogMenuIds.FAVORITES
    }

    fun onSearchChanged(query: String) {
        searchQuery.value = query
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                isRefreshing.value = true
            }

            try {
                withContext(dispatcher) {
                    repository.refresh()
                }
            } finally {
                if (!silent) {
                    isRefreshing.value = false
                }
            }
        }
    }

    fun toggleFavorite(channel: LiveChannel) {
        viewModelScope.launch {
            withContext(dispatcher) {
                userLibraryRepository.toggleFavorite(
                    kind = LibraryItemKind.Live,
                    itemId = channel.streamId.toString(),
                )
            }
        }
    }

    fun recordLastLiveChannel(channel: LiveChannel) {
        viewModelScope.launch {
            withContext(dispatcher) {
                userLibraryRepository.recordLastLiveChannel(channel.streamId)
            }
        }
    }

    private fun buildUiState(
        categories: List<LiveCategory>,
        channels: List<LiveChannel>,
        metadata: CatalogMetadata,
        favoriteIds: Set<String>,
        lastPlaybackId: String?,
        controls: LiveControls,
    ): LiveUiState {
        val menuItems = liveMenuItems(categories)
        val selectedMenuId = controls.selectedMenuItemId
            .takeIf { id -> menuItems.any { item -> item.id == id } }
            ?: CatalogMenuIds.FAVORITES
        val selectedCategoryId = CatalogMenuIds.categoryId(selectedMenuId)
        val favoriteChannelIds = favoriteIds.mapNotNull(String::toIntOrNull).toSet()
        val normalizedQuery = controls.searchQuery.trim()
        val visibleChannels = channels
            .filter { channel ->
                when {
                    selectedMenuId == CatalogMenuIds.FAVORITES -> channel.streamId in favoriteChannelIds
                    selectedCategoryId != null -> channel.categoryId == selectedCategoryId
                    else -> true
                }
            }
            .filter { channel ->
                normalizedQuery.isBlank() ||
                    channel.name.contains(normalizedQuery, ignoreCase = true)
            }
            .let { filteredChannels ->
                val lastLiveChannelId = lastPlaybackId?.toIntOrNull()
                if (selectedMenuId == CatalogMenuIds.FAVORITES && lastLiveChannelId != null) {
                    filteredChannels.sortedByDescending { channel -> channel.streamId == lastLiveChannelId }
                } else {
                    filteredChannels
                }
            }

        return LiveUiState(
            categories = categories,
            menuItems = menuItems,
            visibleChannels = visibleChannels,
            favoriteChannelIds = favoriteChannelIds,
            lastLiveChannelId = lastPlaybackId?.toIntOrNull(),
            selectedMenuItemId = selectedMenuId,
            selectedCategoryId = selectedCategoryId,
            searchQuery = controls.searchQuery,
            searchSuggestions = searchSuggestions(
                query = normalizedQuery,
                names = visibleChannels.map { channel -> channel.name },
            ),
            metadata = metadata,
            isRefreshing = controls.isRefreshing,
            blockingError = metadata.lastRefreshError.takeIf {
                categories.isEmpty() && channels.isEmpty()
            },
        )
    }

    private fun liveMenuItems(categories: List<LiveCategory>): List<CatalogMenuItem> =
        listOf(
            CatalogMenuItem(
                id = CatalogMenuIds.FAVORITES,
                title = "Favoris",
                type = CatalogMenuItemType.Favorites,
            ),
        ) + categories.map { category ->
            CatalogMenuItem(
                id = CatalogMenuIds.category(category.id),
                title = category.name,
                type = CatalogMenuItemType.Category,
                categoryId = category.id,
            )
        }

    private fun searchSuggestions(
        query: String,
        names: List<String>,
    ): List<String> {
        if (query.isBlank()) {
            return emptyList()
        }

        return names
            .asSequence()
            .filter { name -> name.contains(query, ignoreCase = true) }
            .distinctBy { name -> name.lowercase() }
            .take(MAX_SEARCH_SUGGESTIONS)
            .toList()
    }

    private companion object {
        const val MAX_SEARCH_SUGGESTIONS = 6
    }
}

private data class LiveControls(
    val selectedMenuItemId: String,
    val searchQuery: String,
    val isRefreshing: Boolean,
)
