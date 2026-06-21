package com.futuresound.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.futuresound.player.data.Song
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)

enum class RepeatMode { OFF, ONE, ALL }

/**
 * Bridges the app's UI to [PlaybackService] via a Media3 [MediaController].
 * This is the standard, lifecycle-correct way to talk to a
 * MediaSessionService — connecting through a MediaController (rather than
 * holding a raw ExoPlayer reference across process/component boundaries)
 * ensures the service starts itself, promotes to foreground, and tears
 * down correctly without us needing to manage that by hand.
 *
 * Call [connect] once (e.g. from MainActivity.onCreate) before issuing any
 * playback commands, and [disconnect] when the controller is no longer
 * needed (e.g. onDestroy) to release the binding.
 */
object PlaybackBridge {

    private var controller: MediaController? = null

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var currentQueue: List<Song> = emptyList()
    private var pendingQueueAction: (() -> Unit)? = null

    /** Establishes the MediaController connection to PlaybackService. */
    fun connect(context: Context) {
        if (controller != null) return

        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            val mediaController = future.get()
            controller = mediaController
            attachListener(mediaController)
            // Run any playback command requested before the controller finished connecting.
            pendingQueueAction?.invoke()
            pendingQueueAction = null
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        controller?.release()
        controller = null
    }

    private fun attachListener(mediaController: MediaController) {
        _audioSessionId.value = mediaController.sessionExtras.getInt(EXTRA_AUDIO_SESSION_ID, 0)

        mediaController.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = mediaController.currentMediaItemIndex
                val song = currentQueue.getOrNull(index)
                _uiState.value = _uiState.value.copy(
                    currentSong = song,
                    durationMs = mediaController.duration.coerceAtLeast(0L)
                )
                // Re-read session extras on each track transition as a
                // simple, well-documented way to pick up an audio session id
                // that may have been assigned/rotated after connection.
                val id = mediaController.sessionExtras.getInt(EXTRA_AUDIO_SESSION_ID, 0)
                if (id != 0) _audioSessionId.value = id
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                _audioSessionId.value = audioSessionId
            }
        })
    }

    /** Loads a full queue and starts playback at the given start index. */
    fun playQueue(songs: List<Song>, startIndex: Int) {
        val action = {
            val c = controller
            if (c != null) {
                currentQueue = songs
                val items = songs.map { mediaItemFromUri(it.contentUri, it.title, it.artist) }
                c.setMediaItems(items, startIndex, 0L)
                c.prepare()
                c.play()
                _uiState.value = _uiState.value.copy(
                    queue = songs,
                    currentSong = songs.getOrNull(startIndex)
                )
            }
        }
        if (controller != null) action() else pendingQueueAction = action
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun skipToNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        val c = controller ?: return
        val newState = !_uiState.value.shuffleEnabled
        c.shuffleModeEnabled = newState
        _uiState.value = _uiState.value.copy(shuffleEnabled = newState)
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        c.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        _uiState.value = _uiState.value.copy(repeatMode = next)
    }

    /** Polls current playback position; call from a UI-side ticker. */
    fun currentPositionMs(): Long = controller?.currentPosition ?: 0L

    const val EXTRA_AUDIO_SESSION_ID = "com.futuresound.player.AUDIO_SESSION_ID"
}
