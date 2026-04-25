/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Audio processor that produces an Apple-Music-style "Sing"/karaoke effect
 * by attenuating the centre channel of a stereo signal.
 *
 * Technique (Center-Channel Extraction, a.k.a. OOPS — Out-Of-Phase Stereo):
 *   Vocals in commercial music are almost always panned dead-centre, so they
 *   appear with equal amplitude in the L and R channels. Bass and kick drums
 *   are usually centred too, while most other instruments have stereo width.
 *
 *   We split the signal into:
 *      mid  = (L + R) / 2     (centre — vocals, bass, kick)
 *      side = (L - R) / 2     (stereo content — guitars, keys, reverb tails)
 *
 *   We then high-pass the mid signal so kick/bass survive, low-pass it so
 *   air/cymbal centre information survives, and only attenuate the vocal
 *   band (~200 Hz–6 kHz). The remaining mid is recombined with the full side.
 *
 * This avoids the "buzz" / "silence" failure mode of stacked notch filters,
 * which is what the previous implementation suffered from.
 */
@UnstableApi
class VocalSuppressionAudioProcessor : BaseAudioProcessor() {

    @Volatile private var vocalSuppressionEnabled = false
    // 0.0 = no suppression, 1.0 = fully cancel vocal band of the centre channel
    @Volatile private var suppressionStrength = 0.85f

    private var sampleRate = 44100
    private var channelCount = 2

    // Per-channel filters used to isolate the vocal band of the mid signal.
    private var midHighPass: BiquadFilter? = null
    private var midLowPass: BiquadFilter? = null

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {
        // We can only do centre-channel cancellation on 16-bit PCM stereo.
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount

        if (channelCount == 2) {
            // High-pass at ~180 Hz keeps bass/kick out of the suppressed band.
            midHighPass = BiquadFilter.highPass(180f, sampleRate, 0.707f)
            // Low-pass at ~6 kHz keeps cymbal/air content intact.
            midLowPass = BiquadFilter.lowPass(6000f, sampleRate, 0.707f)
        } else {
            midHighPass = null
            midLowPass = null
        }

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // Pass-through when disabled or when the source isn't stereo
        // (centre cancellation requires two channels).
        if (!vocalSuppressionEnabled || channelCount != 2) {
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
            return
        }

        processStereo(inputBuffer)
    }

    private fun processStereo(inputBuffer: ByteBuffer) {
        val byteCount = inputBuffer.remaining()
        // 2 bytes/sample, 2 channels => 4 bytes per stereo frame.
        val frameCount = byteCount / 4

        val output = replaceOutputBuffer(byteCount)

        // Read in little-endian (PCM 16-bit on Android is little-endian).
        val srcOrder = inputBuffer.order()
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        val hp = midHighPass!!
        val lp = midLowPass!!
        val strength = suppressionStrength

        for (i in 0 until frameCount) {
            val l = inputBuffer.short.toFloat()
            val r = inputBuffer.short.toFloat()

            val mid = (l + r) * 0.5f
            val side = (l - r) * 0.5f

            // Vocal band of the mid signal (HP -> LP cascaded).
            val vocalBand = lp.process(hp.process(mid))
            val nonVocalMid = mid - vocalBand
            val suppressedMid = nonVocalMid + vocalBand * (1f - strength)

            // Recombine. Keep full stereo width from the side channel.
            var outL = suppressedMid + side
            var outR = suppressedMid - side

            // Soft clipping safety net (shouldn't trigger in practice, since
            // suppression only removes energy, but guards against edge cases).
            if (outL > 32767f) outL = 32767f else if (outL < -32768f) outL = -32768f
            if (outR > 32767f) outR = 32767f else if (outR < -32768f) outR = -32768f

            output.putShort(outL.toInt().toShort())
            output.putShort(outR.toInt().toShort())
        }

        // Restore caller's byte order on the input buffer to be polite.
        inputBuffer.order(srcOrder)
        output.flip()
    }

    override fun onFlush() {
        midHighPass?.reset()
        midLowPass?.reset()
    }

    override fun onReset() {
        midHighPass = null
        midLowPass = null
    }

    /** Enable centre-channel vocal suppression. [strength] is clamped to 0..1. */
    fun enable(strength: Float = 0.85f) {
        suppressionStrength = strength.coerceIn(0f, 1f)
        vocalSuppressionEnabled = true
        flush()
    }

    /** Disable vocal suppression (audio will pass through untouched). */
    fun disable() {
        vocalSuppressionEnabled = false
        flush()
    }

    fun isEnabled(): Boolean = vocalSuppressionEnabled
    fun getSuppressionStrength(): Float = suppressionStrength

    /**
     * Generic biquad filter (RBJ cookbook). Used for high-pass + low-pass
     * around the vocal band of the mid channel.
     */
    private class BiquadFilter(
        private val b0: Float,
        private val b1: Float,
        private val b2: Float,
        private val a1: Float,
        private val a2: Float,
    ) {
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        fun process(x0: Float): Float {
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0
            return y0
        }

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }

        companion object {
            fun highPass(freq: Float, sampleRate: Int, q: Float): BiquadFilter {
                val w0 = 2f * PI.toFloat() * freq / sampleRate
                val cosW = cos(w0)
                val alpha = sin(w0) / (2f * q)
                val a0 = 1f + alpha
                val b0 = ((1f + cosW) / 2f) / a0
                val b1 = (-(1f + cosW)) / a0
                val b2 = ((1f + cosW) / 2f) / a0
                val a1 = (-2f * cosW) / a0
                val a2 = (1f - alpha) / a0
                return BiquadFilter(b0, b1, b2, a1, a2)
            }

            fun lowPass(freq: Float, sampleRate: Int, q: Float): BiquadFilter {
                val w0 = 2f * PI.toFloat() * freq / sampleRate
                val cosW = cos(w0)
                val alpha = sin(w0) / (2f * q)
                val a0 = 1f + alpha
                val b0 = ((1f - cosW) / 2f) / a0
                val b1 = (1f - cosW) / a0
                val b2 = ((1f - cosW) / 2f) / a0
                val a1 = (-2f * cosW) / a0
                val a2 = (1f - alpha) / a0
                return BiquadFilter(b0, b1, b2, a1, a2)
            }
        }
    }
}
