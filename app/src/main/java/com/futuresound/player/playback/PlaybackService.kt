package com.futuresound.player.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.futuresound.player.MainActivity

/**
 * Foreground service hosting the ExoPlayer instance and MediaSession.
 * Keeps audio playing when the app is backgrounded/screen is off, and
 * surfaces lock-screen / notification playback controls automatically
 * via Media3's session integration. Media3 promotes this service to the
 * foreground automatically once playback starts via a connected
 * MediaController — no manual startForeground() management needed here.
 */
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true) // auto-pause on headphone unplug
            .build()

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .setExtras(audioSessionExtras(player.audioSessionId))
            .build()

        // Keep published session extras in sync whenever ExoPlayer rotates
        // its internal audio session id, so the connected MediaController
        // (and therefore the Visualizer) always has the current id.
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                mediaSession?.setSessionExtras(audioSessionExtras(audioSessionId))
            }
        })
    }

    private fun audioSessionExtras(sessionId: Int): Bundle = Bundle().apply {
        putInt(PlaybackBridge.EXTRA_AUDIO_SESSION_ID, sessionId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop service if nothing is playing when the app is swiped away,
        // matching standard music app behavior; keeps playing otherwise.
        if (!player.isPlaying) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }
}

/** Helper to build a MediaItem from a Song's content URI. */
fun mediaItemFromUri(uri: String, title: String, artist: String): MediaItem =
    MediaItem.Builder()
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .build()
        )
        .build()
