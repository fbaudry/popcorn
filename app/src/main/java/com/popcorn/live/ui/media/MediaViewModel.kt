package com.popcorn.live.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorn.live.catalog.CatalogMetadata
import com.popcorn.live.catalog.MediaCatalogRepository
import com.popcorn.live.catalog.MediaCategory
import com.popcorn.live.catalog.MediaItem
import com.popcorn.live.catalog.MediaKind
import com.popcorn.live.catalog.SeriesDetails
import com.popcorn.live.ui.navigation.CatalogMenuIds
import com.popcorn.live.ui.navigation.CatalogMenuItem
import com.popcorn.live.ui.navigation.CatalogMenuItemType
import com.popcorn.live.user.PlaybackProgress
import com.popcorn.live.user.UserLibraryRepository
import com.popcorn.live.user.toLibraryItemKind
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class MediaViewModel(
    private val kind: MediaKind,
    private val repository: MediaCatalogRepository,
    private val userLibraryRepository: UserLibraryRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val selectedMenuItemId = MutableStateFlow(CatalogMenuIds.FAVORITES)
    private val searchQuery = MutableStateFlow("")
    private val isRefreshing = MutableStateFlow(false)
    private val selectedSeries = MutableStateFlow<MediaItem?>(null)
    private val seriesDetails = MutableStateFlow<SeriesDetails?>(null)
    private val isLoadingDetails = MutableStateFlow(false)
    private val detailsError = MutableStateFlow<String?>(null)

    private val controls = combine(
        selectedMenuItemId,
        searchQuery,
        isRefreshing,
        selectedSeries,
        seriesDetails,
        isLoadingDetails,
        detailsError,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        MediaControls(
            selectedMenuItemId = values[0] as String,
            searchQuery = values[1] as String,
            isRefreshing = values[2] as Boolean,
            selectedSeries = values[3] as MediaItem?,
            seriesDetails = values[4] as SeriesDetails?,
            isLoadingDetails = values[5] as Boolean,
            detailsError = values[6] as String?,
        )
    }

    val uiState = combine(
        repository.categories(kind),
        repository.items(kind),
        repository.metadata(kind),
        userLibraryRepository.favorites(kind.toLibraryItemKind()),
        userLibraryRepository.progress(kind.toLibraryItemKind()),
        controls,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        buildUiState(
            categories = values[0] as List<MediaCategory>,
            items = values[1] as List<MediaItem>,
            metadata = values[2] as CatalogMetadata,
            favoriteIds = values[3] as Set<String>,
            progress = values[4] as List<PlaybackProgress>,
            controls = values[5] as MediaControls,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MediaUiState())

    init {
        refresh(silent = true)
    }

    fun onMenuItemSelected(menuItemId: String) {
        selectedMenuItemId.value = menuItemId
        clearSeriesSelection()
    }

    fun selectFavorites() {
        selectedMenuItemId.value = CatalogMenuIds.FAVORITES
        clearSeriesSelection()
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
                    repository.refresh(kind)
                }
            } finally {
                if (!silent) {
                    isRefreshing.value = false
                }
            }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            withContext(dispatcher) {
                userLibraryRepository.toggleFavorite(
                    kind = item.kind.toLibraryItemKind(),
                    itemId = item.id.toString(),
                )
            }
        }
    }

    fun onSeriesSelected(series: MediaItem) {
        if (kind != MediaKind.Series) {
            return
        }

        selectedSeries.value = series
        seriesDetails.value = null
        detailsError.value = null
        isLoadingDetails.value = true

        viewModelScope.launch {
            runCatching {
                withContext(dispatcher) {
                    repository.seriesDetails(series)
                }
            }.onSuccess { details ->
                seriesDetails.value = details
            }.onFailure { throwable ->
                detailsError.value = throwable.message
                    ?.takeIf(String::isNotBlank)
                    ?: "Impossible de charger les épisodes."
            }
            isLoadingDetails.value = false
        }
    }

    fun clearSeriesSelection() {
        selectedSeries.value = null
        seriesDetails.value = null
        detailsError.value = null
        isLoadingDetails.value = false
    }

    private fun buildUiState(
        categories: List<MediaCategory>,
        items: List<MediaItem>,
        metadata: CatalogMetadata,
        favoriteIds: Set<String>,
        progress: List<PlaybackProgress>,
        controls: MediaControls,
    ): MediaUiState {
        val menuItems = mediaMenuItems(categories)
        val selectedMenuId = controls.selectedMenuItemId
            .takeIf { id -> menuItems.any { item -> item.id == id } }
            ?: CatalogMenuIds.FAVORITES
        val selectedCategoryId = CatalogMenuIds.categoryId(selectedMenuId)
        val favoriteItemIds = favoriteIds.mapNotNull(String::toIntOrNull).toSet()
        val normalizedQuery = controls.searchQuery.trim()
        val baseItems = items
            .filter { item ->
                when {
                    selectedMenuId == CatalogMenuIds.FAVORITES -> item.id in favoriteItemIds
                    selectedMenuId == CatalogMenuIds.RESUME -> false
                    selectedCategoryId != null -> item.categoryId == selectedCategoryId
                    else -> true
                }
            }
        val visibleItems = baseItems
            .filter { item ->
                normalizedQuery.isBlank() ||
                    item.name.contains(normalizedQuery, ignoreCase = true)
            }
        val resumeItems = progress
            .filter { item ->
                normalizedQuery.isBlank() ||
                    item.title.contains(normalizedQuery, ignoreCase = true)
            }

        return MediaUiState(
            categories = categories,
            menuItems = menuItems,
            visibleItems = visibleItems,
            favoriteItemIds = favoriteItemIds,
            progressByItemId = progress.associateBy { item -> item.itemId },
            resumeItems = resumeItems,
            selectedMenuItemId = selectedMenuId,
            selectedCategoryId = selectedCategoryId,
            searchQuery = controls.searchQuery,
            searchSuggestions = searchSuggestions(
                query = normalizedQuery,
                names = if (selectedMenuId == CatalogMenuIds.RESUME) {
                    resumeItems.map { item -> item.title }
                } else {
                    visibleItems.map { item -> item.name }
                },
            ),
            metadata = metadata,
            isRefreshing = controls.isRefreshing,
            blockingError = metadata.lastRefreshError.takeIf {
                categories.isEmpty() && items.isEmpty()
            },
            selectedSeries = controls.selectedSeries,
            seriesDetails = controls.seriesDetails,
            isLoadingDetails = controls.isLoadingDetails,
            detailsError = controls.detailsError,
        )
    }

    private fun mediaMenuItems(categories: List<MediaCategory>): List<CatalogMenuItem> =
        listOf(
            CatalogMenuItem(
                id = CatalogMenuIds.FAVORITES,
                title = "Favoris",
                type = CatalogMenuItemType.Favorites,
            ),
            CatalogMenuItem(
                id = CatalogMenuIds.RESUME,
                title = "Reprendre",
                type = CatalogMenuItemType.Resume,
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

class MoviesViewModel(
    repository: MediaCatalogRepository,
    userLibraryRepository: UserLibraryRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MediaViewModel(MediaKind.Movies, repository, userLibraryRepository, dispatcher)

class SeriesViewModel(
    repository: MediaCatalogRepository,
    userLibraryRepository: UserLibraryRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MediaViewModel(MediaKind.Series, repository, userLibraryRepository, dispatcher)

private data class MediaControls(
    val selectedMenuItemId: String,
    val searchQuery: String,
    val isRefreshing: Boolean,
    val selectedSeries: MediaItem?,
    val seriesDetails: SeriesDetails?,
    val isLoadingDetails: Boolean,
    val detailsError: String?,
)
