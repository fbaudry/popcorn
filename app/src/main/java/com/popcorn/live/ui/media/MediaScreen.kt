package com.popcorn.live.ui.media

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popcorn.live.catalog.MediaItem
import com.popcorn.live.catalog.MediaKind
import com.popcorn.live.catalog.SeriesEpisode
import com.popcorn.live.ui.components.CatalogSearchField
import com.popcorn.live.ui.components.ChannelLogo
import com.popcorn.live.ui.navigation.AppSection
import com.popcorn.live.ui.navigation.CatalogMenuIds
import com.popcorn.live.ui.navigation.CatalogMenuItem
import com.popcorn.live.ui.navigation.SectionSelector
import com.popcorn.live.ui.theme.CyanDeep
import com.popcorn.live.ui.theme.ElectricCyan
import com.popcorn.live.ui.theme.Obsidian
import com.popcorn.live.ui.theme.OutlineGhost
import com.popcorn.live.ui.theme.SurfaceHigh
import com.popcorn.live.ui.theme.SurfaceHighest
import com.popcorn.live.ui.theme.SurfaceLow
import com.popcorn.live.ui.theme.TextPrimary
import com.popcorn.live.ui.theme.TextSecondary
import com.popcorn.live.user.PlaybackProgress
import kotlinx.coroutines.delay

@Composable
fun MediaScreen(
    section: AppSection,
    kind: MediaKind,
    state: MediaUiState,
    selectedSection: AppSection,
    onSectionSelected: (AppSection) -> Unit,
    onMenuItemSelected: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onItemSelected: (MediaItem) -> Unit,
    onFavoriteToggled: (MediaItem) -> Unit,
    onResumeSelected: (PlaybackProgress) -> Unit,
    onBackFromDetails: () -> Unit,
    onEpisodeSelected: (MediaItem, SeriesEpisode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Obsidian, Color(0xFF101827), Obsidian),
                ),
            )
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        MediaSidebar(
            section = section,
            menuItems = state.menuItems,
            selectedMenuItemId = state.selectedMenuItemId,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected,
            onMenuItemSelected = onMenuItemSelected,
        )
        Spacer(Modifier.width(18.dp))
        Column(Modifier.fillMaxSize()) {
            Header(
                title = section.title,
                searchPlaceholder = if (kind == MediaKind.Movies) "Rechercher un film" else "Rechercher une série",
                searchQuery = state.searchQuery,
                searchSuggestions = state.searchSuggestions,
                isRefreshing = state.isRefreshing,
                refreshError = state.metadata.lastRefreshError,
                hasCachedCatalog = state.categories.isNotEmpty() || state.visibleItems.isNotEmpty(),
                onSearchChanged = onSearchChanged,
                onRefresh = onRefresh,
            )
            Spacer(Modifier.height(14.dp))
            when {
                state.selectedSeries != null -> SeriesDetailsContent(
                    series = state.selectedSeries,
                    state = state,
                    onBack = onBackFromDetails,
                    onEpisodeSelected = onEpisodeSelected,
                )
                state.blockingError != null -> BlockingMessage(
                    title = "Catalogue indisponible",
                    message = state.blockingError,
                )
                state.selectedMenuItemId == CatalogMenuIds.RESUME && state.resumeItems.isEmpty() -> BlockingMessage(
                    title = "Aucune reprise",
                    message = if (state.searchQuery.isBlank()) {
                        "Les films et épisodes commencés apparaîtront ici."
                    } else {
                        "Aucune reprise ne correspond à ta recherche."
                    },
                )
                state.selectedMenuItemId == CatalogMenuIds.RESUME -> ResumeGrid(
                    items = state.resumeItems,
                    onResumeSelected = onResumeSelected,
                )
                state.visibleItems.isEmpty() -> BlockingMessage(
                    title = mediaEmptyTitle(kind, state),
                    message = mediaEmptyMessage(kind, state),
                )
                else -> MediaGrid(
                    items = state.visibleItems,
                    favoriteItemIds = state.favoriteItemIds,
                    onItemSelected = onItemSelected,
                    onFavoriteToggled = onFavoriteToggled,
                )
            }
        }
    }
}

@Composable
private fun MediaSidebar(
    section: AppSection,
    menuItems: List<CatalogMenuItem>,
    selectedMenuItemId: String,
    selectedSection: AppSection,
    onSectionSelected: (AppSection) -> Unit,
    onMenuItemSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(224.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceLow.copy(alpha = 0.86f))
            .border(BorderStroke(1.dp, OutlineGhost), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(
            text = "POPCORN",
            color = ElectricCyan,
            style = MaterialTheme.typography.headlineLarge,
            maxLines = 1,
        )
        Text(
            text = section.title.uppercase(),
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(Modifier.height(12.dp))
        SectionSelector(
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "MENU",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(9.dp))
        if (menuItems.isEmpty()) {
            Text(
                text = "Chargement du catalogue",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 10.dp),
            ) {
                items(menuItems, key = { it.id }) { item ->
                    SidebarItem(
                        title = item.title,
                        selected = item.id == selectedMenuItemId,
                        onClick = { onMenuItemSelected(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val backgroundColor = when {
        focused -> SurfaceHighest
        selected -> ElectricCyan.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val borderColor = when {
        focused -> ElectricCyan
        selected -> ElectricCyan.copy(alpha = 0.36f)
        else -> Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, borderColor), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (selected || focused) ElectricCyan else Color.Transparent),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = if (selected || focused) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Header(
    title: String,
    searchPlaceholder: String,
    searchQuery: String,
    searchSuggestions: List<String>,
    isRefreshing: Boolean,
    refreshError: String?,
    hasCachedCatalog: Boolean,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.displayLarge,
                    maxLines = 1,
                )
                Text(
                    text = statusText(
                        isRefreshing = isRefreshing,
                        refreshError = refreshError,
                        hasCachedCatalog = hasCachedCatalog,
                    ),
                    color = if (refreshError == null) TextSecondary else ElectricCyan,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            RefreshButton(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
            )
        }
        Spacer(Modifier.height(12.dp))
        CatalogSearchField(
            searchQuery = searchQuery,
            placeholder = searchPlaceholder,
            suggestions = searchSuggestions,
            onSearchChanged = onSearchChanged,
        )
    }
}

@Composable
private fun RefreshButton(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val background = if (focused) {
        Brush.horizontalGradient(listOf(ElectricCyan, CyanDeep))
    } else {
        Brush.horizontalGradient(listOf(SurfaceHigh, SurfaceHigh))
    }
    val contentColor = if (focused) Color.Black else TextPrimary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(48.dp)
            .height(48.dp)
            .clip(shape)
            .background(background)
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) ElectricCyan else OutlineGhost),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onRefresh,
            )
            .focusable(interactionSource = interactionSource)
            .semantics {
                contentDescription = if (isRefreshing) {
                    "Rafraîchissement en cours"
                } else {
                    "Rafraîchir"
                }
            },
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun MediaGrid(
    items: List<MediaItem>,
    favoriteItemIds: Set<Int>,
    onItemSelected: (MediaItem) -> Unit,
    onFavoriteToggled: (MediaItem) -> Unit,
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    val firstItemKey = items.firstOrNull()?.let { item -> "${item.kind.storageKey}-${item.id}" }

    LaunchedEffect(firstItemKey) {
        if (firstItemKey != null) {
            delay(150)
            firstItemFocusRequester.requestFocus()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 126.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(items, key = { _, item -> "${item.kind.storageKey}-${item.id}" }) { index, item ->
            MediaCard(
                item = item,
                isFavorite = item.id in favoriteItemIds,
                focusRequester = if (index == 0) firstItemFocusRequester else null,
                onItemSelected = onItemSelected,
                onFavoriteToggled = onFavoriteToggled,
            )
        }
    }
}

@Composable
private fun MediaCard(
    item: MediaItem,
    isFavorite: Boolean,
    focusRequester: FocusRequester?,
    onItemSelected: (MediaItem) -> Unit,
    onFavoriteToggled: (MediaItem) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val background = if (focused) {
        Brush.linearGradient(listOf(SurfaceHighest, Color(0xFF173A52)))
    } else {
        Brush.linearGradient(listOf(SurfaceHigh, Color(0xFF17202D)))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) ElectricCyan else OutlineGhost),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { onItemSelected(item) },
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
        ) {
            ChannelLogo(
                name = item.name,
                imageUrl = item.posterUrl,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp,
                contentPadding = 0.dp,
                framed = false,
                fallbackStyle = MaterialTheme.typography.displayLarge,
                contentScale = ContentScale.Crop,
            )
            FavoriteButton(
                isFavorite = isFavorite,
                onClick = { onFavoriteToggled(item) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color.Black.copy(alpha = 0.34f))
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = item.name,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ResumeGrid(
    items: List<PlaybackProgress>,
    onResumeSelected: (PlaybackProgress) -> Unit,
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    val firstItemKey = items.firstOrNull()?.let { item -> "${item.kind.storageKey}-${item.itemId}" }

    LaunchedEffect(firstItemKey) {
        if (firstItemKey != null) {
            delay(150)
            firstItemFocusRequester.requestFocus()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 126.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(items, key = { _, item -> "${item.kind.storageKey}-${item.itemId}" }) { index, item ->
            ResumeCard(
                item = item,
                focusRequester = if (index == 0) firstItemFocusRequester else null,
                onResumeSelected = onResumeSelected,
            )
        }
    }
}

@Composable
private fun ResumeCard(
    item: PlaybackProgress,
    focusRequester: FocusRequester?,
    onResumeSelected: (PlaybackProgress) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val background = if (focused) {
        Brush.linearGradient(listOf(SurfaceHighest, Color(0xFF173A52)))
    } else {
        Brush.linearGradient(listOf(SurfaceHigh, Color(0xFF17202D)))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) ElectricCyan else OutlineGhost),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { onResumeSelected(item) },
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource),
    ) {
        ChannelLogo(
            name = item.title,
            imageUrl = item.imageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            cornerRadius = 0.dp,
            contentPadding = 0.dp,
            framed = false,
            fallbackStyle = MaterialTheme.typography.displayLarge,
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.Black.copy(alpha = 0.34f))
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                text = item.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = progressText(item),
                color = ElectricCyan,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val backgroundColor = when {
        focused -> ElectricCyan
        isFavorite -> ElectricCyan.copy(alpha = 0.24f)
        else -> Color.Black.copy(alpha = 0.42f)
    }
    val contentColor = if (focused) Color.Black else ElectricCyan

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(34.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, ElectricCyan.copy(alpha = 0.52f)), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .semantics {
                contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris"
            },
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = if (isFavorite || focused) contentColor else TextSecondary,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun SeriesDetailsContent(
    series: MediaItem,
    state: MediaUiState,
    onBack: () -> Unit,
    onEpisodeSelected: (MediaItem, SeriesEpisode) -> Unit,
) {
    val details = state.seriesDetails
    BackHandler(onBack = onBack)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceLow.copy(alpha = 0.74f))
            .border(BorderStroke(1.dp, OutlineGhost), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier
                .width(160.dp)
                .fillMaxHeight(),
        ) {
            ChannelLogo(
                name = details?.name ?: series.name,
                imageUrl = details?.coverUrl ?: series.posterUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
                cornerRadius = 14.dp,
                contentPadding = 0.dp,
                framed = false,
                fallbackStyle = MaterialTheme.typography.displayLarge,
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(10.dp))
            CompactButton(
                label = "Retour",
                onClick = onBack,
            )
        }
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight(),
        ) {
            Text(
                text = details?.name ?: series.name,
                color = TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            MediaMetaLine(
                genre = details?.genre ?: series.genre,
                rating = details?.rating ?: series.rating,
                releaseDate = details?.releaseDate ?: series.releaseDate,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = details?.plot ?: series.plot ?: "Aucun résumé disponible.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 7,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            when {
                state.isLoadingDetails -> BlockingMessage(
                    title = "Chargement",
                    message = "Récupération des épisodes Xtream.",
                )
                state.detailsError != null -> BlockingMessage(
                    title = "Épisodes indisponibles",
                    message = state.detailsError,
                )
                details == null || details.episodes.isEmpty() -> BlockingMessage(
                    title = "Aucun épisode",
                    message = "Cette série ne contient pas encore d'épisodes lisibles.",
                )
                else -> EpisodeList(
                    series = series,
                    episodes = details.episodes,
                    onEpisodeSelected = onEpisodeSelected,
                )
            }
        }
    }
}

@Composable
private fun MediaMetaLine(
    genre: String?,
    rating: String?,
    releaseDate: String?,
) {
    val parts = listOfNotNull(
        genre?.takeIf(String::isNotBlank),
        rating?.takeIf(String::isNotBlank)?.let { "Note $it" },
        releaseDate?.takeIf(String::isNotBlank),
    )
    Text(
        text = parts.joinToString("  •  ").ifBlank { "Détails non disponibles" },
        color = ElectricCyan,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun EpisodeList(
    series: MediaItem,
    episodes: List<SeriesEpisode>,
    onEpisodeSelected: (MediaItem, SeriesEpisode) -> Unit,
) {
    val firstEpisodeFocusRequester = remember { FocusRequester() }
    val firstEpisodeKey = episodes.firstOrNull()?.id

    LaunchedEffect(firstEpisodeKey) {
        if (firstEpisodeKey != null) {
            delay(150)
            firstEpisodeFocusRequester.requestFocus()
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(episodes, key = { "${it.season}-${it.episodeNumber}-${it.id}" }) { episode ->
            EpisodeRow(
                series = series,
                episode = episode,
                focusRequester = if (episode.id == firstEpisodeKey) firstEpisodeFocusRequester else null,
                onEpisodeSelected = onEpisodeSelected,
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    series: MediaItem,
    episode: SeriesEpisode,
    focusRequester: FocusRequester?,
    onEpisodeSelected: (MediaItem, SeriesEpisode) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val backgroundColor = if (focused) SurfaceHighest else SurfaceHigh
    val borderColor = if (focused) ElectricCyan else OutlineGhost

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, borderColor), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { onEpisodeSelected(series, episode) },
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp),
    ) {
        Text(
            text = episodeLabel(episode),
            color = ElectricCyan,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(74.dp),
        )
        Text(
            text = episode.title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CompactButton(
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val background = if (focused) {
        Brush.horizontalGradient(listOf(ElectricCyan, CyanDeep))
    } else {
        Brush.horizontalGradient(listOf(SurfaceHigh, SurfaceHigh))
    }
    val contentColor = if (focused) Color.Black else TextPrimary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) ElectricCyan else OutlineGhost), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
        )
    }
}

@Composable
private fun BlockingMessage(
    title: String,
    message: String,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceLow.copy(alpha = 0.74f))
            .border(BorderStroke(1.dp, OutlineGhost), RoundedCornerShape(22.dp))
            .padding(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun episodeLabel(episode: SeriesEpisode): String =
    if (episode.season > 0) {
        "S%02dE%02d".format(episode.season, episode.episodeNumber)
    } else {
        "E%02d".format(episode.episodeNumber)
    }

private fun mediaEmptyTitle(kind: MediaKind, state: MediaUiState): String = when {
    state.selectedMenuItemId == CatalogMenuIds.FAVORITES -> "Aucun favori"
    kind == MediaKind.Movies -> "Aucun film"
    else -> "Aucune série"
}

private fun mediaEmptyMessage(kind: MediaKind, state: MediaUiState): String = when {
    state.searchQuery.isNotBlank() -> "Aucun résultat ne correspond à ta recherche."
    state.selectedMenuItemId == CatalogMenuIds.FAVORITES -> {
        val label = if (kind == MediaKind.Movies) "films" else "séries"
        "Ajoute des $label avec l'étoile pour les retrouver ici."
    }
    else -> "Rafraîchis le catalogue Xtream ou choisis une autre catégorie."
}

private fun progressText(item: PlaybackProgress): String {
    if (item.durationMillis <= 0L) {
        return "Reprendre"
    }

    val percent = ((item.positionMillis * 100) / item.durationMillis)
        .coerceIn(1L, 99L)
    return "Reprendre à $percent%"
}

private fun statusText(
    isRefreshing: Boolean,
    refreshError: String?,
    hasCachedCatalog: Boolean,
): String = when {
    isRefreshing -> "Rafraîchissement du catalogue Xtream"
    refreshError != null && hasCachedCatalog -> "Catalogue en cache affiché. Dernier refresh échoué : $refreshError"
    refreshError != null -> "Refresh échoué : $refreshError"
    hasCachedCatalog -> "Catalogue prêt pour la navigation à la télécommande"
    else -> "Chargement du catalogue Xtream"
}
