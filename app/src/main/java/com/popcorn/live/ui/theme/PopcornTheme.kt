package com.popcorn.live.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Obsidian = Color(0xFF0C0E12)
val SurfaceLow = Color(0xFF121720)
val SurfaceHigh = Color(0xFF1B2230)
val SurfaceHighest = Color(0xFF253044)
val ElectricCyan = Color(0xFF00E5FF)
val CyanDeep = Color(0xFF145CFF)
val NeonViolet = Color(0xFF875BFF)
val TextPrimary = Color(0xFFF4F7FB)
val TextSecondary = Color(0xFFAAABB0)
val OutlineGhost = Color(0x33F4F7FB)

private val PopcornColors = darkColorScheme(
    background = Obsidian,
    surface = SurfaceLow,
    surfaceVariant = SurfaceHigh,
    primary = ElectricCyan,
    secondary = CyanDeep,
    tertiary = NeonViolet,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    onPrimary = Color.Black,
    onSecondary = TextPrimary,
)

private val PopcornTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun PopcornTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PopcornColors,
        typography = PopcornTypography,
        content = content,
    )
}
