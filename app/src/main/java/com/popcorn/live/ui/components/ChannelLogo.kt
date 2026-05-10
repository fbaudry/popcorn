package com.popcorn.live.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.popcorn.live.ui.theme.ElectricCyan

@Composable
fun ChannelLogo(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    contentPadding: Dp = 5.dp,
    framed: Boolean = true,
    fallbackStyle: TextStyle? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val url = imageUrl?.trim()?.takeIf(String::isNotBlank)
    val shape = RoundedCornerShape(cornerRadius)
    val backgroundColor = if (framed) ElectricCyan.copy(alpha = 0.14f) else Color.Transparent
    val borderColor = if (framed) ElectricCyan.copy(alpha = 0.32f) else Color.Transparent

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = shape),
    ) {
        if (url == null) {
            LogoFallback(name = name, fallbackStyle = fallbackStyle)
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = contentScale,
                loading = {
                    LogoFallback(name = name, fallbackStyle = fallbackStyle)
                },
                error = {
                    LogoFallback(name = name, fallbackStyle = fallbackStyle)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}

@Composable
private fun LogoFallback(
    name: String,
    fallbackStyle: TextStyle?,
) {
    Text(
        text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        color = ElectricCyan,
        style = fallbackStyle ?: MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
    )
}
