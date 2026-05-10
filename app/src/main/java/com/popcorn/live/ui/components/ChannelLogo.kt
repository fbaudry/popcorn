package com.popcorn.live.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popcorn.live.ui.theme.ElectricCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.LinkedHashMap
import kotlin.math.max
import kotlin.math.roundToInt

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
    val logo = rememberChannelLogo(imageUrl)
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
        if (logo != null) {
            Image(
                bitmap = logo,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        } else {
            Text(
                text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = ElectricCyan,
                style = fallbackStyle ?: MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun rememberChannelLogo(imageUrl: String?): ImageBitmap? {
    val url = imageUrl?.trim()?.takeIf(String::isNotBlank)
    var logo by remember(url) { mutableStateOf(url?.let(ChannelLogoCache::get)) }

    LaunchedEffect(url) {
        if (url == null || logo != null || ChannelLogoCache.hasFailed(url)) {
            return@LaunchedEffect
        }

        val loaded = loadLogo(url)
        if (loaded == null) {
            ChannelLogoCache.markFailed(url)
        } else {
            ChannelLogoCache.put(url, loaded)
            logo = loaded
        }
    }

    return logo
}

private suspend fun loadLogo(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 3_000
            readTimeout = 5_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Popcorn/0.1")
        }

        try {
            if (connection.responseCode !in 200..299) {
                return@withContext null
            }
            if (connection.contentLength > CHANNEL_LOGO_MAX_DOWNLOAD_BYTES) {
                return@withContext null
            }

            connection.inputStream.use { input ->
                val bytes = input.readAtMost(CHANNEL_LOGO_MAX_DOWNLOAD_BYTES) ?: return@withContext null
                decodeBoundedChannelLogo(bytes)?.asImageBitmap()
            }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

internal fun InputStream.readAtMost(maxBytes: Int): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0

    while (true) {
        val read = read(buffer)
        if (read == -1) {
            return output.toByteArray()
        }

        totalBytes += read
        if (totalBytes > maxBytes) {
            return null
        }
        output.write(buffer, 0, read)
    }
}

internal fun decodeBoundedChannelLogo(
    bytes: ByteArray,
    maxDimension: Int = CHANNEL_LOGO_MAX_DIMENSION,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    val decodeOptions = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inSampleSize = sampleSizeFor(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxDimension = maxDimension,
        )
    }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null
    val largestSide = max(decoded.width, decoded.height)
    if (largestSide <= maxDimension) {
        return decoded
    }

    val scale = maxDimension.toFloat() / largestSide.toFloat()
    val scaledWidth = max(1, (decoded.width * scale).roundToInt())
    val scaledHeight = max(1, (decoded.height * scale).roundToInt())
    val scaled = Bitmap.createScaledBitmap(decoded, scaledWidth, scaledHeight, true)
    if (scaled != decoded) {
        decoded.recycle()
    }
    return scaled
}

private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
    var sampleSize = 1
    while (
        width / (sampleSize * 2) >= maxDimension ||
        height / (sampleSize * 2) >= maxDimension
    ) {
        sampleSize *= 2
    }
    return sampleSize
}

internal const val CHANNEL_LOGO_MAX_DIMENSION = 512
internal const val CHANNEL_LOGO_MAX_DOWNLOAD_BYTES = 4 * 1024 * 1024

private object ChannelLogoCache {
    private const val MAX_ENTRIES = 160

    private val images = Collections.synchronizedMap(
        object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean =
                size > MAX_ENTRIES
        },
    )
    private val failedUrls = Collections.synchronizedSet(mutableSetOf<String>())

    fun get(url: String): ImageBitmap? = images[url]

    fun put(url: String, image: ImageBitmap) {
        images[url] = image
    }

    fun hasFailed(url: String): Boolean = failedUrls.contains(url)

    fun markFailed(url: String) {
        failedUrls.add(url)
    }
}
