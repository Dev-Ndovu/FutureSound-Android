package com.futuresound.player.theme

import androidx.compose.ui.graphics.Color

/**
 * Visual "mood" controls how gradients, glows, and particles behave —
 * this is what makes themes feel categorically different, not just recolored.
 */
enum class ThemeMood {
    NEON_CYBERPUNK,
    GLASS_AURORA,
    DEEP_SPACE,
    MINIMAL_GLOW,
    SUNSET_SYNTH,
    MATRIX_PULSE,
    HOLOGRAM,
    LIQUID_CHROME
}

/** Shape language for buttons, cards, and the now-playing artwork frame. */
enum class ShapeStyle {
    SHARP_ANGULAR,
    ROUNDED_SOFT,
    HEX_FACETED,
    CIRCUIT_CUT
}

/** Which visualizer render mode looks best paired with this theme by default. */
enum class VisualizerStyle {
    BARS,
    RADIAL_PULSE,
    WAVEFORM_LINE,
    PARTICLE_FIELD,
    RIBBON
}

data class FutureTheme(
    val id: Int,
    val name: String,
    val mood: ThemeMood,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val shapeStyle: ShapeStyle,
    val defaultVisualizerStyle: VisualizerStyle,
    val glowIntensity: Float, // 0f..1f, drives blur/shadow radius on accents
    val particleDensity: Float // 0f..1f, drives how busy ambient background motion is
)
