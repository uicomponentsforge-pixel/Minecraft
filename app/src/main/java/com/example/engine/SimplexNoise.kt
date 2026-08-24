package com.example.engine

import kotlin.math.floor

/**
 * Fast Simplex & Fractal Perlin-style Noise Generator
 */
class SimplexNoise(seed: Long = 1337L) {

    private val p = IntArray(512)
    private val perm = IntArray(512)
    private val permMod12 = IntArray(512)

    init {
        val rand = java.util.Random(seed)
        val source = (0..255).toList().shuffled(rand)
        for (i in 0..255) {
            p[i] = source[i]
            p[256 + i] = source[i]
            perm[i] = source[i]
            perm[256 + i] = source[i]
            permMod12[i] = (source[i] % 12)
            permMod12[256 + i] = (source[i] % 12)
        }
    }

    private val grad3 = arrayOf(
        floatArrayOf(1f, 1f, 0f), floatArrayOf(-1f, 1f, 0f), floatArrayOf(1f, -1f, 0f), floatArrayOf(-1f, -1f, 0f),
        floatArrayOf(1f, 0f, 1f), floatArrayOf(-1f, 0f, 1f), floatArrayOf(1f, 0f, -1f), floatArrayOf(-1f, 0f, -1f),
        floatArrayOf(0f, 1f, 1f), floatArrayOf(0f, -1f, 1f), floatArrayOf(0f, 1f, -1f), floatArrayOf(0f, -1f, -1f)
    )

    private fun dot(g: FloatArray, x: Float, y: Float): Float = g[0] * x + g[1] * y
    private fun dot(g: FloatArray, x: Float, y: Float, z: Float): Float = g[0] * x + g[1] * y + g[2] * z

    fun eval2D(xin: Float, yin: Float): Float {
        val f2 = 0.5f * (Math.sqrt(3.0) - 1.0).toFloat()
        val g2 = (3.0 - Math.sqrt(3.0)).toFloat() / 6.0f

        val s = (xin + yin) * f2
        val i = floor(xin + s).toInt()
        val j = floor(yin + s).toInt()
        val t = (i + j) * g2
        val x0 = xin - (i - t)
        val y0 = yin - (j - t)

        val i1: Int
        val j1: Int
        if (x0 > y0) {
            i1 = 1; j1 = 0
        } else {
            i1 = 0; j1 = 1
        }

        val x1 = x0 - i1 + g2
        val y1 = y0 - j1 + g2
        val x2 = x0 - 1.0f + 2.0f * g2
        val y2 = y0 - 1.0f + 2.0f * g2

        val ii = i and 255
        val jj = j and 255
        val gi0 = permMod12[ii + perm[jj]]
        val gi1 = permMod12[ii + i1 + perm[jj + j1]]
        val gi2 = permMod12[ii + 1 + perm[jj + 1]]

        var n0 = 0f
        var n1 = 0f
        var n2 = 0f

        var t0 = 0.5f - x0 * x0 - y0 * y0
        if (t0 > 0) {
            t0 *= t0
            n0 = t0 * t0 * dot(grad3[gi0], x0, y0)
        }

        var t1 = 0.5f - x1 * x1 - y1 * y1
        if (t1 > 0) {
            t1 *= t1
            n1 = t1 * t1 * dot(grad3[gi1], x1, y1)
        }

        var t2 = 0.5f - x2 * x2 - y2 * y2
        if (t2 > 0) {
            t2 *= t2
            n2 = t2 * t2 * dot(grad3[gi2], x2, y2)
        }

        return 70.0f * (n0 + n1 + n2)
    }

    fun eval3D(xin: Float, yin: Float, zin: Float): Float {
        val f3 = 1.0f / 3.0f
        val s = (xin + yin + zin) * f3
        val i = floor(xin + s).toInt()
        val j = floor(yin + s).toInt()
        val k = floor(zin + s).toInt()

        val g3 = 1.0f / 6.0f
        val t = (i + j + k) * g3
        val x0 = xin - (i - t)
        val y0 = yin - (j - t)
        val z0 = zin - (k - t)

        val i1: Int; val j1: Int; val k1: Int
        val i2: Int; val j2: Int; val k2: Int
        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0
            } else if (x0 >= z0) {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1
            } else {
                i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1
            }
        } else {
            if (y0 < z0) {
                i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1
            } else if (x0 < z0) {
                i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1
            } else {
                i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0
            }
        }

        val x1 = x0 - i1 + g3
        val y1 = y0 - j1 + g3
        val z1 = z0 - k1 + g3
        val x2 = x0 - i2 + 2.0f * g3
        val y2 = y0 - j2 + 2.0f * g3
        val z2 = z0 - k2 + 2.0f * g3
        val x3 = x0 - 1.0f + 3.0f * g3
        val y3 = y0 - 1.0f + 3.0f * g3
        val z3 = z0 - 1.0f + 3.0f * g3

        val ii = i and 255
        val jj = j and 255
        val kk = k and 255

        val gi0 = permMod12[ii + perm[jj + perm[kk]]]
        val gi1 = permMod12[ii + i1 + perm[jj + j1 + perm[kk + k1]]]
        val gi2 = permMod12[ii + i2 + perm[jj + j2 + perm[kk + k2]]]
        val gi3 = permMod12[ii + 1 + perm[jj + 1 + perm[kk + 1]]]

        var n0 = 0f; var n1 = 0f; var n2 = 0f; var n3 = 0f

        var t0 = 0.6f - x0 * x0 - y0 * y0 - z0 * z0
        if (t0 > 0) {
            t0 *= t0
            n0 = t0 * t0 * dot(grad3[gi0], x0, y0, z0)
        }

        var t1 = 0.6f - x1 * x1 - y1 * y1 - z1 * z1
        if (t1 > 0) {
            t1 *= t1
            n1 = t1 * t1 * dot(grad3[gi1], x1, y1, z1)
        }

        var t2 = 0.6f - x2 * x2 - y2 * y2 - z2 * z2
        if (t2 > 0) {
            t2 *= t2
            n2 = t2 * t2 * dot(grad3[gi2], x2, y2, z2)
        }

        var t3 = 0.6f - x3 * x3 - y3 * y3 - z3 * z3
        if (t3 > 0) {
            t3 *= t3
            n3 = t3 * t3 * dot(grad3[gi3], x3, y3, z3)
        }

        return 32.0f * (n0 + n1 + n2 + n3)
    }

    /**
     * Fractal Octave Noise
     */
    fun fractal2D(x: Float, y: Float, octaves: Int = 4, persistence: Float = 0.5f, lacunarity: Float = 2.0f): Float {
        var total = 0f
        var frequency = 1.0f
        var amplitude = 1.0f
        var maxValue = 0f
        for (i in 0 until octaves) {
            total += eval2D(x * frequency, y * frequency) * amplitude
            maxValue += amplitude
            amplitude *= persistence
            frequency *= lacunarity
        }
        return total / maxValue
    }
}
