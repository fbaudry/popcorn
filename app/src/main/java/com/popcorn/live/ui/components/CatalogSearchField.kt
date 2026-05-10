package com.popcorn.live.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.popcorn.live.ui.theme.ElectricCyan
import com.popcorn.live.ui.theme.OutlineGhost
import com.popcorn.live.ui.theme.SurfaceHigh
import com.popcorn.live.ui.theme.SurfaceHighest
import com.popcorn.live.ui.theme.SurfaceLow
import com.popcorn.live.ui.theme.TextPrimary
import com.popcorn.live.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun CatalogSearchField(
    searchQuery: String,
    placeholder: String,
    suggestions: List<String>,
    onSearchChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val visibleSuggestions = remember(searchQuery, suggestions) {
        suggestions.take(MAX_VISIBLE_SUGGESTIONS)
    }

    BackHandler(enabled = isEditing) {
        isEditing = false
        keyboardController?.hide()
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
            delay(100)
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (isEditing) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                singleLine = true,
                placeholder = { Text(placeholder) },
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        isEditing = false
                        keyboardController?.hide()
                    },
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceLow,
                    unfocusedContainerColor = SurfaceLow.copy(alpha = 0.82f),
                    cursorColor = ElectricCyan,
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = OutlineGhost,
                    focusedPlaceholderColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .focusRequester(focusRequester),
            )
            if (visibleSuggestions.isNotEmpty()) {
                SuggestionList(
                    suggestions = visibleSuggestions,
                    onSuggestionSelected = { suggestion ->
                        onSearchChanged(suggestion)
                        isEditing = false
                        keyboardController?.hide()
                    },
                )
            }
        } else {
            SearchDisplayField(
                searchQuery = searchQuery,
                placeholder = placeholder,
                onActivate = { isEditing = true },
            )
        }
    }
}

@Composable
private fun SearchDisplayField(
    searchQuery: String,
    placeholder: String,
    onActivate: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(SurfaceLow.copy(alpha = 0.82f))
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) ElectricCyan else OutlineGhost), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onActivate,
            )
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) {
                    onActivate()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = searchQuery.ifBlank { placeholder },
            color = if (searchQuery.isBlank()) TextSecondary else TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SuggestionList(
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(top = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 184.dp),
    ) {
        items(suggestions, key = { it }) { suggestion ->
            SuggestionRow(
                suggestion = suggestion,
                onSuggestionSelected = onSuggestionSelected,
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: String,
    onSuggestionSelected: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(shape)
            .background(if (focused) SurfaceHighest else SurfaceHigh)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) ElectricCyan else OutlineGhost), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { onSuggestionSelected(suggestion) },
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp),
    ) {
        Text(
            text = suggestion,
            color = if (focused) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val MAX_VISIBLE_SUGGESTIONS = 6
