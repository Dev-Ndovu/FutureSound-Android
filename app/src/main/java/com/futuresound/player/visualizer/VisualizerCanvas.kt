package com.futuresound.player.visualizer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.futuresound.player.theme.FutureTheme
import com.futuresound.player.theme.VisualizerStyle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders the live [AudioFrame] from [VisualizerEngine] using the render
 * style and color palette dictated by the active [FutureTheme]. All styles
 * read the same underlying FFT/waveform data — only the drawing differs.
 */
@Composable
fun VisualizerCanvas(
    engine: VisualizerEngine,
    theme: FutureTheme,
    style: VisualizerStyle = theme.defaultVisualizerStyle,
    modifier: Modifier = Modifier
) {
    val frame by engine.audioFrame.collectAsState()

    // Slow continuous rotation/drift used by radial + particle styles so
    // they feel alive even during quiet passages.
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_drift")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift_angle"
    )

    Canvas(modifier = modifier.fillMaxSize().background(Color.Transparent)) {
        when (style) {
            VisualizerStyle.BARS -> drawBars(frame.magnitudes, theme)
            VisualizerStyle.RADIAL_PULSE -> drawRadialPulse(frame.magnitudes, theme, drift)
            VisualizerStyle.WAVEFORM_LINE -> drawWaveformLine(frame.waveform, theme)
            VisualizerStyle.PARTICLE_FIELD -> drawParticleField(frame.magnitudes, theme, drift)
            VisualizerStyle.RIBBON -> drawRibbon(frame.waveform, theme, drift)
        }
    }
}

private fun DrawScope.drawBars(
    magnitudes: FloatArray,
    theme: FutureTheme
) {
    val barCount = magnitudes.size
    val gap = 4f
    val barWidth = (size.width - gap * (barCount - 1)) / barCount
    val maxBarHeight = size.height * 0.9f

    for (i in 0 until barCount) {
        val mag = magnitudes[i]
        val barHeight = (mag * maxBarHeight).coerceAtLeast(4f)
        val x = i * (barWidth + gap)
        val y = size.height - barHeight

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(theme.accent, theme.primary),
                startY = y,
                endY = size.height
            ),
            topLeft = Offset(x, y),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
        )
    }
}

private fun DrawScope.drawRadialPulse(
    magnitudes: FloatArray,
    theme: FutureTheme,
    driftDegrees: Float
) {
    val center = Offset(size.width / 2, size.height / 2)
    val baseRadius = min(size.width, size.height) * 0.25f
    val maxExtra = min(size.width, size.height) * 0.22f
    val barCount = magnitudes.size

    for (i in 0 until barCount) {
        val mag = magnitudes[i]
        val angle = Math.toRadians((i * (360f / barCount) + driftDegrees).toDouble())
        val innerR = baseRadius
        val outerR = baseRadius + maxExtra * mag.coerceAtLeast(0.05f)

        val start = Offset(
            x = center.x + (cos(angle) * innerR).toFloat(),
            y = center.y + (sin(angle) * innerR).toFloat()
        )
        val end = Offset(
            x = center.x + (cos(angle) * outerR).toFloat(),
            y = center.y + (sin(angle) * outerR).toFloat()
        )

        drawLine(
            color = lerpColor(theme.primary, theme.accent, mag),
            start = start,
            end = end,
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }

    // Soft core glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(theme.accent.copy(alpha = theme.glowIntensity * 0.5f), Color.Transparent)
        ),
        radius = baseRadius * 0.9f,
        center = center
    )
}

private fun DrawScope.drawWaveformLine(
    waveform: FloatArray,
    theme: FutureTheme
) {
    if (waveform.isEmpty()) return
    val midY = size.height / 2
    val stepX = size.width / (waveform.size - 1).coerceAtLeast(1)
    val amplitudeScale = size.height * 0.4f

    val path = androidx.compose.ui.graphics.Path()
    waveform.forEachIndexed { i, sample ->
        val x = i * stepX
        val y = midY + sample * amplitudeScale
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = theme.accent,
        style = Stroke(width = 5f, cap = StrokeCap.Round)
    )
    drawPath(
        path = path,
        color = theme.primary.copy(alpha = 0.4f),
        style = Stroke(width = 14f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawParticleField(
    magnitudes: FloatArray,
    theme: FutureTheme,
    driftDegrees: Float
) {
    val center = Offset(size.width / 2, size.height / 2)
    val maxRadius = min(size.width, size.height) * 0.45f
    val count = magnitudes.size

    for (i in 0 until count) {
        val mag = magnitudes[i]
        val angle = Math.toRadians(((i * (360f / count)) + driftDegrees * (1f + i % 3 * 0.1f)).toDouble())
        val radius = maxRadius * (0.3f + mag * 0.7f)
        val pos = Offset(
            x = center.x + (cos(angle) * radius).toFloat(),
            y = center.y + (sin(angle) * radius).toFloat()
        )
        val particleSize = (3f + mag * 10f) * (0.5f + theme.particleDensity)

        drawCircle(
            color = lerpColor(theme.secondary, theme.accent, mag).copy(alpha = 0.85f),
            radius = particleSize,
            center = pos
        )
    }
}

private fun DrawScope.drawRibbon(
    waveform: FloatArray,
    theme: FutureTheme,
    driftDegrees: Float
) {
    if (waveform.isEmpty()) return
    val midY = size.height / 2
    val stepX = size.width / (waveform.size - 1).coerceAtLeast(1)
    val amplitudeScale = size.height * 0.35f
    val phaseShift = (driftDegrees / 360f) * stepX * 4f

    val topPath = androidx.compose.ui.graphics.Path()
    val bottomPath = androidx.compose.ui.graphics.Path()

    waveform.forEachIndexed { i, sample ->
        val x = i * stepX
        val yTop = midY + sample * amplitudeScale - 6f + phaseShift * 0.1f
        val yBottom = midY + sample * amplitudeScale + 6f
        if (i == 0) {
            topPath.moveTo(x, yTop)
            bottomPath.moveTo(x, yBottom)
        } else {
            topPath.lineTo(x, yTop)
            bottomPath.lineTo(x, yBottom)
        }
    }

    drawPath(topPath, brush = Brush.horizontalGradient(listOf(theme.primary, theme.accent)), style = Stroke(width = 8f, cap = StrokeCap.Round))
    drawPath(bottomPath, brush = Brush.horizontalGradient(listOf(theme.accent, theme.secondary)), style = Stroke(width = 8f, cap = StrokeCap.Round))
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = 1f
    )
}
