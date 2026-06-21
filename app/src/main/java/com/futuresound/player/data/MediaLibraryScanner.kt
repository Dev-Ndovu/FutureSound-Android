package com.futuresound.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans the device's local storage for audio files using MediaStore and
 * builds an in-memory library. Runs fully offline — no network calls.
 */
class MediaLibraryScanner(private val context: Context) {

    /**
     * Queries MediaStore.Audio for all playable music files on the device.
     * Excludes very short clips (e.g. notification sounds, voice memos < 30s)
     * unless the device has very few tracks overall.
     */
    suspend fun scanLibrary(minDurationMs: Long = 30_000L): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val duration = cursor.getLong(durationCol)
                if (duration < minDurationMs) continue

                val albumId = cursor.getLong(albumIdCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                )

                songs += Song(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown Title",
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    album = cursor.getString(albumCol) ?: "Unknown Album",
                    durationMs = duration,
                    albumArtUri = albumArtUri.toString(),
                    contentUri = contentUri.toString(),
                    sizeBytes = cursor.getLong(sizeCol),
                    dateAddedSec = cursor.getLong(dateAddedCol)
                )
            }
        }

        songs
    }

    fun groupByAlbum(songs: List<Song>): List<Album> =
        songs.groupBy { it.album to it.artist }
            .map { (key, songList) ->
                Album(
                    name = key.first,
                    artist = key.second,
                    albumArtUri = songList.firstOrNull()?.albumArtUri,
                    songs = songList
                )
            }
            .sortedBy { it.name }

    fun groupByArtist(songs: List<Song>): List<Artist> =
        songs.groupBy { it.artist }
            .map { (name, songList) -> Artist(name = name, songs = songList) }
            .sortedBy { it.name }
}
