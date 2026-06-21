# FutureSound — Native Android Music Player

A from-scratch native Kotlin/Jetpack Compose offline music player with:
- Local media library scanning (MediaStore, no network)
- Background playback via Media3/ExoPlayer + foreground service (lock-screen controls included)
- **256 procedurally generated futuristic themes** (8 palettes × 8 moods × 4 variants)
- **Real-time FFT audio visualizer** (5 render styles: bars, radial pulse, waveform line, particle field, ribbon)

## How to open this project

1. Install **Android Studio** (Ladybug or newer recommended).
2. Open Android Studio → "Open" → select the `futuresound/` folder (the one containing `build.gradle.kts` and `settings.gradle.kts`).
3. Let Gradle sync. It will download the Gradle 8.7 distribution and all dependencies automatically (needs internet on first sync).
4. Run on a device or emulator running **API 26+** (Android 8.0+).
5. Grant the music permission prompt when the app launches — it scans MediaStore for local audio files.

> Note: this project was built in a sandboxed environment without network/Gradle access, so it has **not been compiled here**. The code follows standard, current Media3/Compose APIs, but please run a Gradle sync and fix any version-drift issues (e.g. if a newer Compose BOM has a breaking API change) before relying on it.

## Architecture

```
data/          MediaStore scanning, Song/Album/Artist models, reactive repository
playback/      ExoPlayer + MediaSessionService (foreground, lock-screen controls) + PlaybackBridge (UI-facing state)
theme/         FutureTheme model, ThemeGenerator (256 themes), DataStore persistence, Compose Material3 bridge
visualizer/    Android Visualizer API wrapper (FFT/waveform capture) + Canvas renderer (5 styles)
ui/            Library screen, Now Playing screen, Theme picker screen, bottom-nav root
```

### How the pieces connect
- `PlaybackService` runs ExoPlayer in a foreground service and exposes its `audioSessionId` through `PlaybackBridge`.
- `VisualizerEngine` attaches to that session id and streams FFT + waveform data as a `StateFlow<AudioFrame>`.
- `VisualizerCanvas` draws that data on a Compose `Canvas`, using colors/glow/density from the **active** `FutureTheme`.
- `ThemeController` (in `FutureSoundApp`) holds the currently selected theme, persisted via DataStore, and is the single source every screen reads from — so switching a theme in the picker instantly re-themes Library, Now Playing, and the visualizer.

## Known next steps / things to verify after first build

1. **Album art loading**: `albumArtUri` is wired through but no image-loading library (e.g. Coil) is included yet — add `io.coil-kt:coil-compose` and an `AsyncImage` call in `LibraryScreen`/`NowPlayingScreen` if you want artwork rendered.
2. **Queue/playlist persistence**: currently in-memory only (lost on process death). Consider Room if you want saved playlists.
3. **Visualizer on some OEM devices**: the `Visualizer` API is known to be flaky on a handful of older Samsung/Xiaomi ROMs. The code fails gracefully (falls back to silence) but test on a real device.
4. **Theme picker performance**: tested logically for 256 items via `LazyVerticalGrid`; verify scroll performance on a low-end device and consider thumbnail caching if it stutters.
5. **Equalizer/audio effects**: not included — could be added via `android.media.audiofx.Equalizer` attached to the same `audioSessionId`.

## Permissions requested

- `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (API 26-32): scan local music
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: background playback
- `POST_NOTIFICATIONS`: playback notification (API 33+ requirement)
- `MODIFY_AUDIO_SETTINGS`: required for the Visualizer API to attach cleanly on some devices
