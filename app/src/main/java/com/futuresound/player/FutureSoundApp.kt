package com.futuresound.player

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.futuresound.player.data.MusicLibraryRepository
import com.futuresound.player.theme.ThemeController

class FutureSoundApp : Application() {

    lateinit var musicLibraryRepository: MusicLibraryRepository
        private set

    lateinit var themeController: ThemeController
        private set

    override fun onCreate() {
        super.onCreate()
        musicLibraryRepository = MusicLibraryRepository(this)
        themeController = ThemeController(this, ProcessLifecycleOwner.get().lifecycleScope)
    }
}
