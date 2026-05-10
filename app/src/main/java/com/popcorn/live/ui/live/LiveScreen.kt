package com.popcorn.live.ui.live

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
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popcorn.live.catalog.LiveCategory
import com.popcorn.live.catalog.LiveChannel
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

@Composable
fun LiveScreen(
    state: LiveUiState,
    selectedSection: AppSection,
    onSectionSelected: (AppSection) -> Unit,
    onMenuItemSelected: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onChannelSelected: (LiveChannel) -> Unit,
    onFavoriteToggled: (LiveChannel) -> Unit,
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
        CategorySidebar(
            menuItems = state.menuItems,
            selectedMenuItemId = state.selectedMenuItemId,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected,
            onMenuItemSelected = onMenuItemSelected,
        )
        Spacer(Modifier.width(18.dp))
        Column(Modifier.fillMaxSize()) {
            Header(
                searchQuery = state.searchQuery,
                searchSuggestions = state.searchSuggestions,
                isRefreshing = state.isRefreshing,
                refreshError = state.metadata.lastRefreshError,
                hasCachedCatalog = state.categories.isNotEmpty() || state.visibleChannels.isNotEmpty(),
                onSearchChanged = onSearchChanged,
                onRefresh = onRefresh,
            )
            Spacer(Modifier.height(14.dp))
            when {
                state.blockingError != null -> BlockingMessage(
                    title = "Catalogue indisponible",
                    message = state.blockingError,
                )
                state.visibleChannels.isEmpty() -> BlockingMessage(
                    title = liveEmptyTitle(state),
                    message = liveEmptyMessage(state),
                )
                else -> ChannelGrid(
                    channels = state.visibleChannels,
                    favoriteChannelIds = state.favoriteChannelIds,
                    onChannelSelected = onChannelSelected,
                    onFavoriteToggled = onFavoriteToggled,
                )
            }
        }
    }
}

@Composable
private fun CategorySidebar(
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
            text = "LIVE TV",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
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
                text = "Waiting for catalog data",
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
                    text = "Live TV",
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
            placeholder = "Rechercher une chaîne",
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
private fun ChannelGrid(
    channels: List<LiveChannel>,
    favoriteChannelIds: Set<Int>,
    onChannelSelected: (LiveChannel) -> Unit,
    onFavoriteToggled: (LiveChannel) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(channels, key = { it.streamId }) { channel ->
            ChannelCard(
                channel = channel,
                isFavorite = channel.streamId in favoriteChannelIds,
                onChannelSelected = onChannelSelected,
                onFavoriteToggled = onFavoriteToggled,
            )
        }
    }
}

@Composable
private fun ChannelCard(
    channel: LiveChannel,
    isFavorite: Boolean,
    onChannelSelected: (LiveChannel) -> Unit,
    onFavoriteToggled: (LiveChannel) -> Unit,
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
            .height(132.dp)
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
                onClick = { onChannelSelected(channel) },
            )
            .focusable(interactionSource = interactionSource)
            .padding(0.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            ChannelLogo(
                name = channel.name,
                imageUrl = channel.streamIcon,
                modifier = Modifier
                    .width(190.dp)
                    .height(78.dp),
                cornerRadius = 12.dp,
                contentPadding = 0.dp,
                framed = false,
                fallbackStyle = MaterialTheme.typography.displayLarge,
            )
            FavoriteButton(
                isFavorite = isFavorite,
                onClick = { onFavoriteToggled(channel) },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = channel.name,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
        else -> Color.Black.copy(alpha = 0.38f)
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

private fun liveEmptyTitle(state: LiveUiState): String = when {
    state.selectedMenuItemId == CatalogMenuIds.FAVORITES -> "Aucun favori"
    else -> "Aucune chaîne"
}

private fun liveEmptyMessage(state: LiveUiState): String = when {
    state.searchQuery.isNotBlank() -> "Aucune chaîne live ne correspond à ta recherche."
    state.selectedMenuItemId == CatalogMenuIds.FAVORITES -> "Ajoute des chaînes avec l'étoile pour les retrouver ici."
    else -> "Rafraîchis le catalogue Xtream ou choisis une autre catégorie."
}

private fun statusText(
    isRefreshing: Boolean,
    refreshError: String?,
    hasCachedCatalog: Boolean,
): String = when {
    isRefreshing -> "Rafraîchissement du catalogue Xtream"
    refreshError != null && hasCachedCatalog -> "Chaînes en cache affichées. Dernier refresh échoué : $refreshError"
    refreshError != null -> "Refresh échoué : $refreshError"
    hasCachedCatalog -> "Catalogue prêt pour la navigation à la télécommande"
    else -> "Chargement du catalogue Xtream"
}
