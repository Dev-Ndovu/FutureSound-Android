package com.futuresound.player.data

/**
 * Represents a single audio track read from the device's MediaStore.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumArtUri: String?,
    val contentUri: String,
    val sizeBytes: Long,
    val dateAddedSec: Long
)

/**
 * A group of songs by album, derived from the flat song list.
 */
data class Album(
    val name: String,
    val artist: String,
    val albumArtUri: String?,
    val songs: List<Song>
)

/**
 * A group of songs by artist, derived from the flat song list.
 */
data class Artist(
    val name: String,
    val songs: List<Song>
)
