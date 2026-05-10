package com.popcorn.live.ui.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.BehindLiveWindowException
import androidx.media3.ui.PlayerView
import com.popcorn.live.R
import com.popcorn.live.ui.components.ChannelLogo
import com.popcorn.live.ui.theme.ElectricCyan
import com.popcorn.live.ui.theme.OutlineGhost
import com.popcorn.live.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class PlayerContent(
    val key: String,
    val title: String,
    val imageUrl: String?,
    val label: String,
)

data class LiveChannelControls(
    val canChangeChannel: Boolean,
    val onPreviousChannel: () -> Unit,
    val onNextChannel: () -> Unit,
)

@Composable
@OptIn(UnstableApi::class)
fun PlayerScreen(
    content: PlayerContent,
    urls: PlaybackUrls,
    onBack: () -> Unit,
    liveChannelControls: LiveChannelControls? = null,
    startPositionMillis: Long = 0L,
    onPlaybackProgress: (positionMillis: Long, durationMillis: Long) -> Unit = { _, _ -> },
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var errorMessage by remember(content.key) { mutableStateOf<String?>(null) }
    val controlsVisibleState = remember { mutableStateOf(true) }
    val controlsVisible by controlsVisibleState
    val player = remember(content.key) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .build()
    }

    DisposableEffect(player, urls) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (error.hasCause<BehindLiveWindowException>()) {
                    errorMessage = null
                    player.seekToDefaultPosition()
                    player.prepare()
                    player.playWhenReady = true
                    return
                }

                errorMessage = "Flux illisible"
            }
        }

        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            reportPlaybackProgress(player, onPlaybackProgress)
            player.release()
        }
    }

    LaunchedEffect(content.key, urls.url, startPositionMillis) {
        errorMessage = null
        controlsVisibleState.value = true
        player.setMediaItem(
            MediaItem.fromUri(urls.url),
            startPositionMillis.coerceAtLeast(0L),
        )
        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(player, content.key) {
        while (isActive) {
            delay(PROGRESS_REPORT_INTERVAL_MILLIS)
            reportPlaybackProgress(player, onPlaybackProgress)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                val layoutId = if (liveChannelControls == null) {
                    R.layout.popcorn_player_view
                } else {
                    R.layout.popcorn_live_player_view
                }
                (LayoutInflater.from(viewContext).inflate(layoutId, null) as PlayerView).apply {
                    this.player = player
                    setEnableComposeSurfaceSyncWorkaround(true)
                    keepScreenOn = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    isFocusable = true
                    isFocusableInTouchMode = true
                    controllerShowTimeoutMs = 3_500
                    if (liveChannelControls != null) {
                        setShowPreviousButton(false)
                        setShowRewindButton(false)
                        setShowFastForwardButton(false)
                        setShowNextButton(false)
                        configureLiveChannelControls(liveChannelControls)
                    }
                    syncControllerVisibility(controlsVisibleState)
                    post { requestFocus() }
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.configureLiveChannelControls(liveChannelControls)
                playerView.syncControllerVisibility(controlsVisibleState)
            },
        )
        if (controlsVisible) {
            PlayerChannelOverlay(
                content = content,
                errorMessage = errorMessage,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp),
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun PlayerView.syncControllerVisibility(controlsVisibleState: MutableState<Boolean>) {
    setControllerVisibilityListener(
        PlayerView.ControllerVisibilityListener { visibility ->
            controlsVisibleState.value = visibility == View.VISIBLE
        },
    )
    controlsVisibleState.value = isControllerFullyVisible
}

private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is T) {
            return true
        }
        current = current.cause
    }
    return false
}

private fun reportPlaybackProgress(
    player: Player,
    onPlaybackProgress: (positionMillis: Long, durationMillis: Long) -> Unit,
) {
    onPlaybackProgress(
        player.currentPosition.coerceAtLeast(0L),
        player.duration.coerceAtLeast(0L),
    )
}

private fun PlayerView.configureLiveChannelControls(controls: LiveChannelControls?) {
    val previousButton = findViewById<View>(R.id.popcorn_channel_previous) ?: return
    val nextButton = findViewById<View>(R.id.popcorn_channel_next) ?: return
    val enabled = controls?.canChangeChannel == true

    previousButton.isEnabled = enabled
    previousButton.isFocusable = enabled
    previousButton.alpha = if (enabled) 1f else DISABLED_CONTROL_ALPHA
    previousButton.setOnClickListener {
        controls?.onPreviousChannel?.invoke()
    }

    nextButton.isEnabled = enabled
    nextButton.isFocusable = enabled
    nextButton.alpha = if (enabled) 1f else DISABLED_CONTROL_ALPHA
    nextButton.setOnClickListener {
        controls?.onNextChannel?.invoke()
    }
}

private const val DISABLED_CONTROL_ALPHA = 0.36f
private const val PROGRESS_REPORT_INTERVAL_MILLIS = 5_000L

@Composable
private fun PlayerChannelOverlay(
    content: PlayerContent,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .border(BorderStroke(1.dp, OutlineGhost), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        ChannelLogo(
            name = content.title,
            imageUrl = content.imageUrl,
            modifier = Modifier.size(36.dp),
            cornerRadius = 9.dp,
            contentPadding = 4.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = content.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = errorMessage ?: content.label,
                color = ElectricCyan,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}
