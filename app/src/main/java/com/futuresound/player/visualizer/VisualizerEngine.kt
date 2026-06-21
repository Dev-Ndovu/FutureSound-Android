package com.futuresound.player.visualizer

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Snapshot of audio analysis data for one frame, used by all visualizer
 * render styles. Magnitudes are normalized 0f..1f for easy rendering.
 */
data class AudioFrame(
    val waveform: FloatArray,   // normalized -1f..1f amplitude samples
    val magnitudes: FloatArray  // normalized 0f..1f frequency bin magnitudes
) {
    companion object {
        fun silent(bins: Int = 64): AudioFrame =
            AudioFrame(FloatArray(bins), FloatArray(bins))
    }
}

/**
 * Wraps Android's [Visualizer] API to expose real-time FFT + waveform data
 * as a StateFlow. Must be attached to an active audio session id (obtained
 * from the playback engine) to receive real data; otherwise emits silence.
 */
class VisualizerEngine {

    private var visualizer: Visualizer? = null

    private val _audioFrame = MutableStateFlow(AudioFrame.silent())
    val audioFrame: StateFlow<AudioFrame> = _audioFrame.asStateFlow()

    /**
     * Attaches to the given audio session. Call this whenever playback
     * starts on a new session id (ExoPlayer exposes this via
     * `exoPlayer.audioSessionId`).
     */
    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId == 0) return // 0 = no valid session yet

        try {
            val viz = Visualizer(audioSessionId)
            viz.setEnabled(false)
            // Largest capture size supported gives the smoothest FFT resolution.
            val captureSize = Visualizer.getCaptureSizeRange()[1]
            viz.captureSize = captureSize

            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform ?: return
                        _audioFrame.value = _audioFrame.value.copy(
                            waveform = normalizeWaveform(waveform)
                        )
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        fft ?: return
                        _audioFrame.value = _audioFrame.value.copy(
                            magnitudes = normalizeFft(fft)
                        )
                    }
                },
                /* rate = */ Visualizer.getMaxCaptureRate() / 2,
                /* waveform = */ true,
                /* fft = */ true
            )

            viz.setEnabled(true)
            visualizer = viz
        } catch (_: Exception) {
            // Visualizer can fail to attach on some OEM devices/sessions;
            // fail gracefully and keep emitting silence rather than crash.
            visualizer = null
        }
    }

    fun release() {
        visualizer?.let {
            try {
                it.setEnabled(false)
                it.release()
            } catch (_: Exception) { }
        }
        visualizer = null
    }

    /** Converts raw 8-bit waveform bytes into normalized -1f..1f floats. */
    private fun normalizeWaveform(raw: ByteArray): FloatArray {
        val bins = 64
        val step = (raw.size / bins).coerceAtLeast(1)
        val result = FloatArray(bins)
        for (i in 0 until bins) {
            val idx = (i * step).coerceAtMost(raw.size - 1)
            // byte range -128..127 -> -1f..1f
            result[i] = raw[idx] / 128f
        }
        return result
    }

    /**
     * Converts raw FFT byte output (interleaved real/imaginary pairs per
     * the Visualizer API spec) into normalized 0f..1f magnitude bins.
     */
    private fun normalizeFft(fft: ByteArray): FloatArray {
        val bins = 64
        val usablePairs = (fft.size / 2).coerceAtLeast(1)
        val step = (usablePairs / bins).coerceAtLeast(1)
        val result = FloatArray(bins)
        var maxMag = 1f

        val rawMags = FloatArray(bins)
        for (i in 0 until bins) {
            val pairIdx = (i * step).coerceAtMost(usablePairs - 1)
            val re = fft[pairIdx * 2].toInt()
            val im = if (pairIdx * 2 + 1 < fft.size) fft[pairIdx * 2 + 1].toInt() else 0
            val magnitude = kotlin.math.sqrt((re * re + im * im).toFloat())
            rawMags[i] = magnitude
            if (magnitude > maxMag) maxMag = magnitude
        }
        for (i in 0 until bins) {
            result[i] = (rawMags[i] / maxMag).coerceIn(0f, 1f)
        }
        return result
    }
}
