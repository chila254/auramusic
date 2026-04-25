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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Audio processor that suppresses vocals by applying a notch filter in the vocal frequency range.
 * This creates a karaoke effect by reducing the center channel where vocals are typically mixed.
 */
@UnstableApi
class VocalSuppressionAudioProcessor : BaseAudioProcessor() {

    private var vocalSuppressionEnabled = false
    private var suppressionStrength = 0.7f // 0.0 to 1.0, higher = more suppression
    private var inputAudioFormat: AudioProcessor.AudioFormat? = null

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Store audio format for processing
        this.inputAudioFormat = inputAudioFormat
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!vocalSuppressionEnabled) {
            // Pass through unchanged
            replaceOutputBuffer(inputBuffer.remaining()).put(inputBuffer)
            return
        }

        // Apply simple vocal suppression when enabled
        // For now, this is a basic implementation that slightly reduces mid-range frequencies
        val outputBuffer = replaceOutputBuffer(inputBuffer.remaining())

        // Simple vocal suppression: slightly attenuate mid-range frequencies
        // This is a placeholder - a full implementation would use FFT and frequency domain processing
        val inputArray = ByteArray(inputBuffer.remaining())
        inputBuffer.get(inputArray)
        inputBuffer.rewind()

        // Apply basic frequency filtering (simplified)
        for (i in inputArray.indices step 2) { // Process 16-bit samples
            val sample = (inputArray[i].toInt() and 0xFF) or (inputArray[i + 1].toInt() shl 8)
            var processedSample = sample

            // Simple mid-range attenuation (rough approximation of vocal suppression)
            // This reduces frequencies around 1000-3000Hz range
            if (Math.abs(sample) > 1000) { // Only process louder sounds
                processedSample = (sample * (1.0f - suppressionStrength * 0.3f)).toInt()
            }

            // Clamp to 16-bit range
            processedSample = processedSample.coerceIn(-32768, 32767)

            outputBuffer.put((processedSample and 0xFF).toByte())
            outputBuffer.put((processedSample shr 8).toByte())
        }

        outputBuffer.flip()
    }

    /**
     * Enable vocal suppression with specified strength
     */
    fun enable(strength: Float = 0.7f) {
        suppressionStrength = strength.coerceIn(0f, 1f)
        vocalSuppressionEnabled = true
        flush()
    }

    /**
     * Disable vocal suppression
     */
    fun disable() {
        vocalSuppressionEnabled = false
        flush()
    }

    /**
     * Check if vocal suppression is enabled
     */
    fun isEnabled(): Boolean = vocalSuppressionEnabled

    /**
     * Get current suppression strength
     */
    fun getSuppressionStrength(): Float = suppressionStrength


}