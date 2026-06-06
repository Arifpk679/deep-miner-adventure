package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

object AudioSynthesizer {
    var enabled: Boolean = true

    fun playTone(frequency: Double, durationMs: Int, type: ToneType = ToneType.SINE) {
        if (!enabled) return
        
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 8000
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)
                
                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / frequency)
                    val value = when(type) {
                        ToneType.SINE -> sin(angle)
                        ToneType.SQUARE -> if (sin(angle) >= 0) 0.4 else -0.4
                        ToneType.TRIANGLE -> {
                            val x = (i * frequency / sampleRate) % 1.0
                            2.0 * kotlin.math.abs(2.0 * (x - kotlin.math.floor(x + 0.5))) - 1.0
                        }
                    }
                    samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                
                delay(durationMs.toLong() + 30)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    enum class ToneType { SINE, SQUARE, TRIANGLE }

    fun playDigProgress() {
        playTone(200.0, 30, ToneType.TRIANGLE)
    }

    fun playDigBreak() {
        playTone(150.0, 100, ToneType.SQUARE)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun playGemCollected() {
        GlobalScope.launch(Dispatchers.IO) {
            playTone(523.25, 60, ToneType.SINE)
            delay(70)
            playTone(659.25, 60, ToneType.SINE)
            delay(70)
            playTone(783.99, 120, ToneType.SINE)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun playSell() {
        GlobalScope.launch(Dispatchers.IO) {
            for (freq in listOf(523.25, 659.25, 783.99)) {
                playTone(freq, 70, ToneType.SINE)
                delay(60)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun playUpgrade() {
        GlobalScope.launch(Dispatchers.IO) {
            for (freq in listOf(392.00, 523.25, 659.25, 783.99, 1046.50)) {
                playTone(freq, 90, ToneType.SINE)
                delay(80)
            }
        }
    }

    fun playLowEnergy() {
        playTone(440.0, 120, ToneType.SQUARE)
    }
}
