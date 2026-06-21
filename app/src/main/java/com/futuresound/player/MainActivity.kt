package com.futuresound.player

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.futuresound.player.playback.PlaybackBridge
import com.futuresound.player.ui.FutureSoundRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private fun readMediaPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = (application as FutureSoundApp).musicLibraryRepository

        PlaybackBridge.connect(applicationContext)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                lifecycleScope.launch { repository.refresh() }
            } else {
                repository.markPermissionNeeded()
            }
        }

        val notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* notification visibility is best-effort; no UI consequence if denied */ }

        setContent {
            var hasRequestedPermission by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (!hasRequestedPermission) {
                    hasRequestedPermission = true
                    permissionLauncher.launch(readMediaPermission())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            FutureSoundRoot(
                onRequestPermission = { permissionLauncher.launch(readMediaPermission()) }
            )
        }
    }

    override fun onDestroy() {
        PlaybackBridge.disconnect()
        super.onDestroy()
    }
}
