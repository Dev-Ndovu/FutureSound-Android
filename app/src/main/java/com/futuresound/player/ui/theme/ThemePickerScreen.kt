package com.futuresound.player.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuresound.player.theme.FutureTheme

/**
 * Grid of every generated theme. Designed to stay smooth scrolling through
 * 250+ entries since LazyVerticalGrid only composes visible items.
 */
@Composable
fun ThemePickerScreen(
    themes: List<FutureTheme>,
    activeThemeId: Int,
    onThemeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Themes (${themes.size})",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(themes, key = { it.id }) { theme ->
                ThemeSwatch(
                    theme = theme,
                    isSelected = theme.id == activeThemeId,
                    onClick = { onThemeSelected(theme.id) }
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: FutureTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(theme.background, theme.surface, theme.primary.copy(alpha = 0.35f))
                )
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) theme.accent else theme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        // Mini glow dot representing accent color
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(50))
                .background(theme.accent)
                .padding(6.dp)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = theme.accent,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Text(
            text = theme.name,
            color = theme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        )
    }
}
