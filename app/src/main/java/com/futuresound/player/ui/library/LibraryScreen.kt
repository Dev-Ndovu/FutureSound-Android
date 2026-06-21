package com.futuresound.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.futuresound.player.data.Song
import com.futuresound.player.theme.FutureTheme

@Composable
fun LibraryScreen(
    songs: List<Song>,
    theme: FutureTheme,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = theme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search songs, artists, albums") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            singleLine = true
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(songs, key = { it.id }) { song ->
                SongRow(song = song, theme = theme, onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, theme: FutureTheme, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(theme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = theme.accent)
        }

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = song.title,
                color = theme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.album}",
                color = theme.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
