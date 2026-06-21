package com.futuresound.player.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class LibraryState {
    object Loading : LibraryState()
    object NeedsPermission : LibraryState()
    data class Loaded(val songs: List<Song>) : LibraryState()
    data class Empty(val reason: String) : LibraryState()
}

/**
 * Single source of truth for the local music library. Holds scanned songs
 * in memory and exposes them as a StateFlow so Compose screens recompose
 * automatically when the library changes (e.g. after a rescan).
 */
class MusicLibraryRepository(context: Context) {

    private val scanner = MediaLibraryScanner(context)

    private val _libraryState = MutableStateFlow<LibraryState>(LibraryState.Loading)
    val libraryState: StateFlow<LibraryState> = _libraryState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    suspend fun refresh() {
        _libraryState.value = LibraryState.Loading
        val songs = scanner.scanLibrary()
        _libraryState.value = if (songs.isEmpty()) {
            LibraryState.Empty("No music files found on this device.")
        } else {
            LibraryState.Loaded(songs)
        }
    }

    fun markPermissionNeeded() {
        _libraryState.value = LibraryState.NeedsPermission
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun filteredSongs(): List<Song> {
        val state = _libraryState.value
        val all = (state as? LibraryState.Loaded)?.songs ?: emptyList()
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return all
        return all.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }

    fun albums(): List<Album> {
        val state = _libraryState.value
        val all = (state as? LibraryState.Loaded)?.songs ?: emptyList()
        return scanner.groupByAlbum(all)
    }

    fun artists(): List<Artist> {
        val state = _libraryState.value
        val all = (state as? LibraryState.Loaded)?.songs ?: emptyList()
        return scanner.groupByArtist(all)
    }
}
