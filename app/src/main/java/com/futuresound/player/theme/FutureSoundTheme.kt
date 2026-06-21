package com.futuresound.player.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Maps a procedurally generated [FutureTheme] onto real Material3 tokens so
 * every standard Compose component (buttons, cards, sliders, nav bars)
 * automatically reflects the active theme with zero per-screen wiring.
 */
@Composable
fun FutureSoundTheme(theme: FutureTheme, content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = theme.primary,
        secondary = theme.secondary,
        tertiary = theme.accent,
        background = theme.background,
        surface = theme.surface,
        onBackground = theme.onSurface,
        onSurface = theme.onSurface,
        onPrimary = theme.background,
        onSecondary = theme.background
    )

    val shapes = shapesFor(theme.shapeStyle)

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        content = content
    )
}

private fun shapesFor(style: ShapeStyle): Shapes = when (style) {
    ShapeStyle.SHARP_ANGULAR -> Shapes(
        extraSmall = CutCornerShape(2.dp),
        small = CutCornerShape(4.dp),
        medium = CutCornerShape(8.dp),
        large = CutCornerShape(12.dp),
        extraLarge = CutCornerShape(20.dp)
    )
    ShapeStyle.ROUNDED_SOFT -> Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp)
    )
    ShapeStyle.HEX_FACETED -> Shapes(
        extraSmall = CutCornerShape(6.dp),
        small = CutCornerShape(10.dp),
        medium = CutCornerShape(16.dp),
        large = CutCornerShape(24.dp),
        extraLarge = CutCornerShape(32.dp)
    )
    ShapeStyle.CIRCUIT_CUT -> Shapes(
        extraSmall = CutCornerShape(0.dp),
        small = CutCornerShape(2.dp),
        medium = CutCornerShape(4.dp),
        large = CutCornerShape(6.dp),
        extraLarge = CutCornerShape(8.dp)
    )
}
