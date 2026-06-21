package com.futuresound.player.theme

import androidx.compose.ui.graphics.Color

/**
 * Generates the full FutureSound theme catalog procedurally.
 *
 * Rather than hand-authoring 250+ themes (unmaintainable) or randomly
 * shifting one base hue (looks repetitive), this combines:
 *   - 8 curated color palettes (each hand-picked for contrast/legibility)
 *   - 8 visual moods (control gradient + glow + particle behavior)
 *   - 4 shape languages
 *   - 5 visualizer pairings
 * ...with deterministic variation seeded per-index, producing 256 themes
 * that are visually distinct in practice, not just hue-rotated.
 */
object ThemeGenerator {

    private data class Palette(
        val name: String,
        val primary: Color,
        val secondary: Color,
        val accent: Color,
        val background: Color,
        val surface: Color,
        val onSurface: Color
    )

    // 8 hand-curated base palettes spanning different "futuristic" families.
    private val palettes = listOf(
        Palette("Neon Violet", Color(0xFFB14EFF), Color(0xFF6E2EE8), Color(0xFF00F0FF), Color(0xFF0A0A14), Color(0xFF15101F), Color(0xFFEDE6FF)),
        Palette("Cyber Magenta", Color(0xFFFF2E92), Color(0xFFB8005C), Color(0xFF00FFC2), Color(0xFF0D0710), Color(0xFF1A0F1C), Color(0xFFFFE3F0)),
        Palette("Aurora Teal", Color(0xFF2EFFD0), Color(0xFF00B894), Color(0xFFFF6AD5), Color(0xFF051210), Color(0xFF0E1F1B), Color(0xFFDFFFF6)),
        Palette("Solar Amber", Color(0xFFFFA72E), Color(0xFFFF6B00), Color(0xFF2EE6FF), Color(0xFF140C04), Color(0xFF221608), Color(0xFFFFEEDC)),
        Palette("Deep Space Blue", Color(0xFF3E7BFF), Color(0xFF1E40C4), Color(0xFFFF3E7B), Color(0xFF03050F), Color(0xFF0A0F22), Color(0xFFE2EBFF)),
        Palette("Matrix Green", Color(0xFF3EFF6B), Color(0xFF14B84A), Color(0xFFFF3EAE), Color(0xFF030A05), Color(0xFF0A170D), Color(0xFFDFFFE6)),
        Palette("Chrome Silver", Color(0xFFD8DEE9), Color(0xFF8FA3C2), Color(0xFF7BFFEA), Color(0xFF0B0D12), Color(0xFF161A22), Color(0xFFF2F5FA)),
        Palette("Sunset Synth", Color(0xFFFF5E8E), Color(0xFFFF8F4D), Color(0xFF8E6BFF), Color(0xFF120710), Color(0xFF1F0F1A), Color(0xFFFFE6EE))
    )

    private val moods = ThemeMood.values()
    private val shapes = ShapeStyle.values()
    private val visualizers = VisualizerStyle.values()

    // Mood name fragments used to build readable theme names.
    private val moodLabel = mapOf(
        ThemeMood.NEON_CYBERPUNK to "Cyberpunk",
        ThemeMood.GLASS_AURORA to "Glass Aurora",
        ThemeMood.DEEP_SPACE to "Deep Space",
        ThemeMood.MINIMAL_GLOW to "Minimal Glow",
        ThemeMood.SUNSET_SYNTH to "Synthwave",
        ThemeMood.MATRIX_PULSE to "Matrix Pulse",
        ThemeMood.HOLOGRAM to "Hologram",
        ThemeMood.LIQUID_CHROME to "Liquid Chrome"
    )

    /** Total catalog size: 8 palettes x 8 moods x 4 variants = 256 themes. */
    const val THEME_COUNT = 256

    private val cache: List<FutureTheme> by lazy { generateAll() }

    fun all(): List<FutureTheme> = cache

    fun byId(id: Int): FutureTheme = cache[id.coerceIn(0, cache.size - 1)]

    private fun generateAll(): List<FutureTheme> {
        val result = ArrayList<FutureTheme>(THEME_COUNT)
        var index = 0
        for (paletteIdx in palettes.indices) {
            val palette = palettes[paletteIdx]
            for (moodIdx in moods.indices) {
                val mood = moods[moodIdx]
                // 4 variants per (palette, mood) pair: rotate shape + visualizer
                // pairing and nudge glow/particle intensity so variants feel
                // distinct rather than identical with a different name.
                for (variant in 0 until 4) {
                    val shape = shapes[(moodIdx + variant) % shapes.size]
                    val visualizer = visualizers[(paletteIdx + variant) % visualizers.size]
                    val glow = baseGlowFor(mood) + (variant * 0.06f)
                    val particles = baseParticleDensityFor(mood) + (variant * 0.05f)

                    result += FutureTheme(
                        id = index,
                        name = themeName(palette.name, mood, variant),
                        mood = mood,
                        primary = palette.primary,
                        secondary = palette.secondary,
                        accent = palette.accent,
                        background = palette.background,
                        surface = palette.surface,
                        onSurface = palette.onSurface,
                        shapeStyle = shape,
                        defaultVisualizerStyle = visualizer,
                        glowIntensity = glow.coerceIn(0.15f, 1f),
                        particleDensity = particles.coerceIn(0f, 1f)
                    )
                    index++
                }
            }
        }
        return result
    }

    private fun themeName(paletteName: String, mood: ThemeMood, variant: Int): String {
        val variantSuffix = when (variant) {
            0 -> ""
            1 -> " II"
            2 -> " III"
            else -> " IV"
        }
        return "$paletteName ${moodLabel[mood]}$variantSuffix"
    }

    private fun baseGlowFor(mood: ThemeMood): Float = when (mood) {
        ThemeMood.NEON_CYBERPUNK -> 0.85f
        ThemeMood.GLASS_AURORA -> 0.55f
        ThemeMood.DEEP_SPACE -> 0.4f
        ThemeMood.MINIMAL_GLOW -> 0.25f
        ThemeMood.SUNSET_SYNTH -> 0.7f
        ThemeMood.MATRIX_PULSE -> 0.6f
        ThemeMood.HOLOGRAM -> 0.65f
        ThemeMood.LIQUID_CHROME -> 0.5f
    }

    private fun baseParticleDensityFor(mood: ThemeMood): Float = when (mood) {
        ThemeMood.NEON_CYBERPUNK -> 0.5f
        ThemeMood.GLASS_AURORA -> 0.7f
        ThemeMood.DEEP_SPACE -> 0.8f
        ThemeMood.MINIMAL_GLOW -> 0.1f
        ThemeMood.SUNSET_SYNTH -> 0.4f
        ThemeMood.MATRIX_PULSE -> 0.65f
        ThemeMood.HOLOGRAM -> 0.55f
        ThemeMood.LIQUID_CHROME -> 0.3f
    }
}
