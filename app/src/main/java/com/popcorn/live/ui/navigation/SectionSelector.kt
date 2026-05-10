package com.popcorn.live.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.popcorn.live.ui.theme.CyanDeep
import com.popcorn.live.ui.theme.ElectricCyan
import com.popcorn.live.ui.theme.OutlineGhost
import com.popcorn.live.ui.theme.SurfaceHigh
import com.popcorn.live.ui.theme.SurfaceHighest
import com.popcorn.live.ui.theme.TextPrimary
import com.popcorn.live.ui.theme.TextSecondary

@Composable
fun SectionSelector(
    selectedSection: AppSection,
    onSectionSelected: (AppSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        AppSection.entries.forEach { section ->
            SectionButton(
                section = section,
                selected = section == selectedSection,
                onSectionSelected = onSectionSelected,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SectionButton(
    section: AppSection,
    selected: Boolean,
    onSectionSelected: (AppSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val background = when {
        focused -> Brush.horizontalGradient(listOf(ElectricCyan, CyanDeep))
        selected -> Brush.horizontalGradient(listOf(ElectricCyan.copy(alpha = 0.24f), SurfaceHighest))
        else -> Brush.horizontalGradient(listOf(SurfaceHigh, SurfaceHigh))
    }
    val contentColor = if (focused) Color.Black else if (selected) TextPrimary else TextSecondary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(background)
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused || selected) ElectricCyan else OutlineGhost),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { onSectionSelected(section) },
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 5.dp),
    ) {
        Text(
            text = section.sidebarLabel,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
