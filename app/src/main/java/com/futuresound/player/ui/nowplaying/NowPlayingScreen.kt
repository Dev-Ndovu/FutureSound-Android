package com.futuresound.player.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.futuresound.player.playback.PlaybackBridge
import com.futuresound.player.playback.RepeatMode
import com.futuresound.player.theme.FutureTheme
import com.futuresound.player.visualizer.VisualizerCanvas
import com.futuresound.player.visualizer.VisualizerEngine
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    theme: FutureTheme,
    visualizerEngine: VisualizerEngine,
    modifier: Modifier = Modifier
) {
    val uiState by PlaybackBridge.uiState.collectAsState()
    val audioSessionId by PlaybackBridge.audioSessionId.collectAsState()

    // Re-attach the visualizer whenever the underlying audio session changes
    // (e.g. first track start, or ExoPlayer rotates session internally).
    LaunchedEffect(audioSessionId) {
        if (audioSessionId != 0) visualizerEngine.attach(audioSessionId)
    }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var userIsSeeking by remember { mutableStateOf(false) }

    // Lightweight position ticker — polls the player every 500ms while
    // playing so the seek bar advances smoothly without needing a full
    // MediaController position-update subscription.
    LaunchedEffect(uiState.isPlaying) {
        while (uiState.isPlaying) {
            if (!userIsSeeking) {
                sliderPosition = PlaybackBridge.currentPositionMs().toFloat()
            }
            delay(500)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(theme.background, theme.surface)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Visualizer takes the top, framed area of the screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(theme.surface)
            ) {
                VisualizerCanvas(
                    engine = visualizerEngine,
                    theme = theme,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = uiState.currentSong?.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = theme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = uiState.currentSong?.artist ?: "Select a track from your library",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = sliderPosition,
                valueRange = 0f..(uiState.durationMs.coerceAtLeast(1L).toFloat()),
                onValueChange = {
                    userIsSeeking = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    PlaybackBridge.seekTo(sliderPosition.toLong())
                    userIsSeeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = theme.accent,
                    activeTrackColor = theme.primary,
                    inactiveTrackColor = theme.surface
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatMillis(sliderPosition.toLong()), color = theme.onSurface.copy(alpha = 0.6f))
                Text(formatMillis(uiState.durationMs), color = theme.onSurface.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { PlaybackBridge.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (uiState.shuffleEnabled) theme.accent else theme.onSurface.copy(alpha = 0.5f)
                    )
                }
                IconButton(onClick = { PlaybackBridge.skipToPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = theme.onSurface, modifier = Modifier.height(36.dp))
                }
                IconButton(onClick = { PlaybackBridge.togglePlayPause() }) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                        contentDescription = "Play/Pause",
                        tint = theme.accent,
                        modifier = Modifier.height(72.dp)
                    )
                }
                IconButton(onClick = { PlaybackBridge.skipToNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = theme.onSurface, modifier = Modifier.height(36.dp))
                }
                IconButton(onClick = { PlaybackBridge.cycleRepeatMode() }) {
                    Icon(
                        imageVector = if (uiState.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (uiState.repeatMode != RepeatMode.OFF) theme.accent else theme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
