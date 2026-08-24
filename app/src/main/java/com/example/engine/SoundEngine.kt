package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.*

class SoundEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050
    private val random = Random()
    var isMuted = false

    private fun playPcm(generator: (Int, Int) -> ShortArray) {
        if (isMuted) return
        scope.launch {
            try {
                val samples = generator(sampleRate, random.nextInt())
                if (samples.isEmpty()) return@launch

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
                // Wait and release
                kotlinx.coroutines.delay((samples.size * 1000L / sampleRate) + 50)
                audioTrack.release()
            } catch (e: Exception) {
                // Ignore audio errors gracefully
            }
        }
    }

    fun playPlace() {
        playPcm { rate, _ ->
            val duration = (rate * 0.08).toInt()
            val data = ShortArray(duration)
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val freq = 240.0 - (t / 0.08) * 120.0 // pitch drop pop
                val env = (1.0 - t / 0.08).pow(1.5)
                val sample = sin(2.0 * Math.PI * freq * t) * env
                data[i] = (sample * 16000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playDig() {
        playPcm { rate, _ ->
            val duration = (rate * 0.06).toInt()
            val data = ShortArray(duration)
            var last = 0.0
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val env = (1.0 - t / 0.06)
                val noise = random.nextDouble() * 2.0 - 1.0
                last = (last * 0.7 + noise * 0.3) // low pass filter
                data[i] = (last * env * 14000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playBreak() {
        playPcm { rate, _ ->
            val duration = (rate * 0.15).toInt()
            val data = ShortArray(duration)
            var last = 0.0
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val env = (1.0 - t / 0.15).pow(0.8)
                val noise = random.nextDouble() * 2.0 - 1.0
                last = (last * 0.6 + noise * 0.4)
                val sub = sin(2.0 * Math.PI * (120.0 - t * 400.0) * t) * 0.5
                val sample = (last * 0.8 + sub * 0.2) * env
                data[i] = (sample * 22000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playStep() {
        playPcm { rate, _ ->
            val duration = (rate * 0.05).toInt()
            val data = ShortArray(duration)
            var last = 0.0
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val env = (1.0 - t / 0.05)
                val noise = random.nextDouble() * 2.0 - 1.0
                last = (last * 0.8 + noise * 0.2)
                data[i] = (last * env * 8000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playJump() {
        playPcm { rate, _ ->
            val duration = (rate * 0.12).toInt()
            val data = ShortArray(duration)
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val freq = 160.0 + (t / 0.12) * 200.0 // pitch swoop up
                val env = sin((t / 0.12) * Math.PI)
                val sample = sin(2.0 * Math.PI * freq * t) * env
                data[i] = (sample * 12000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playHurt() {
        playPcm { rate, _ ->
            val duration = (rate * 0.2).toInt()
            val data = ShortArray(duration)
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val freq = 280.0 - (t / 0.2) * 160.0 // pitch drop "oof"
                val env = (1.0 - t / 0.2).pow(1.2)
                val square = if (sin(2.0 * Math.PI * freq * t) > 0) 1.0 else -1.0
                val sample = square * env
                data[i] = (sample * 16000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playSwordSwing() {
        playPcm { rate, _ ->
            val duration = (rate * 0.14).toInt()
            val data = ShortArray(duration)
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val env = sin((t / 0.14) * Math.PI)
                val noise = random.nextDouble() * 2.0 - 1.0
                val sweep = sin(2.0 * Math.PI * (400.0 + (1.0 - t / 0.14) * 800.0) * t)
                val sample = (noise * 0.6 + sweep * 0.4) * env
                data[i] = (sample * 15000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playCreeperHiss() {
        playPcm { rate, _ ->
            val duration = (rate * 1.5).toInt()
            val data = ShortArray(duration)
            var last = 0.0
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val env = (t / 1.5).pow(1.5) // escalating volume
                val noise = random.nextDouble() * 2.0 - 1.0
                last = (last * 0.4 + noise * 0.6) // high-pass / noisy hiss
                data[i] = (last * env * 22000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playExplosion() {
        playPcm { rate, _ ->
            val duration = (rate * 0.7).toInt()
            val data = ShortArray(duration)
            var rumble = 0.0
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val env = (1.0 - t / 0.7).pow(0.6)
                val noise = random.nextDouble() * 2.0 - 1.0
                rumble = (rumble * 0.85 + noise * 0.15)
                val sub = sin(2.0 * Math.PI * 55.0 * t)
                val sample = ((noise * 0.5 + rumble * 0.3 + sub * 0.2) * env)
                    .coerceIn(-1.0, 1.0)
                data[i] = (sample * 28000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playEat() {
        playPcm { rate, _ ->
            val duration = (rate * 0.3).toInt()
            val data = ShortArray(duration)
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val crunchPhase = (t * 8.0) % 1.0
                val env = (1.0 - crunchPhase).pow(2.0)
                val noise = random.nextDouble() * 2.0 - 1.0
                data[i] = (noise * env * 14000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playBowShoot() {
        playPcm { rate, _ ->
            val duration = (rate * 0.18).toInt()
            val data = ShortArray(duration)
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val freq = 480.0 - (t / 0.18) * 320.0
                val env = (1.0 - t / 0.18).pow(1.2)
                val sample = sin(2.0 * Math.PI * freq * t) * env
                data[i] = (sample * 18000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playCraftSuccess() {
        playPcm { rate, _ ->
            val duration = (rate * 0.35).toInt()
            val data = ShortArray(duration)
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val note1 = sin(2.0 * Math.PI * 523.25 * t) // C5
                val note2 = if (t > 0.12) sin(2.0 * Math.PI * 659.25 * t) else 0.0 // E5
                val note3 = if (t > 0.22) sin(2.0 * Math.PI * 783.99 * t) else 0.0 // G5
                val env = (1.0 - (t / 0.35)).pow(0.8)
                val sample = (note1 * 0.4 + note2 * 0.4 + note3 * 0.5) * env
                data[i] = (sample * 16000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }

    fun playLevelUp() {
        playPcm { rate, _ ->
            val duration = (rate * 0.6).toInt()
            val data = ShortArray(duration)
            val notes = doubleArrayOf(440.0, 554.37, 659.25, 880.0) // A C# E A
            for (i in 0 until duration) {
                val t = i.toDouble() / rate
                val noteIdx = (t / 0.15).toInt().coerceIn(0, 3)
                val freq = notes[noteIdx]
                val noteT = (t % 0.15)
                val env = (1.0 - noteT / 0.15).pow(0.5)
                val sample = sin(2.0 * Math.PI * freq * t) * env
                data[i] = (sample * 18000).toInt().coerceIn(-32767, 32767).toShort()
            }
            data
        }
    }
}
