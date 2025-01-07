package net.timboode.bluesquarespeedometer.services

import android.graphics.Color

class SpeedColorService {
    public fun getSpeedColor(speedUnit: Float): Int {
        return when {
            speedUnit >= 80.0f -> Color.rgb(139, 0, 0)     // Deep red
            speedUnit >= 70.0f -> {
                val t = (speedUnit - 70.0f) / 10.0f     // 70-80 range
                val r = lerp(255, 139, t)
                val g = lerp(0, 0, t)
                val b = lerp(0, 0, t)
                Color.rgb(r.toInt(), g.toInt(), b.toInt())
            }
            speedUnit >= 60.0f -> {
                val t = (speedUnit - 60.0f) / 10.0f     // 60-70 range
                val r = lerp(255, 255, t)
                val g = lerp(140, 0, t)
                val b = lerp(0, 0, t)
                Color.rgb(r.toInt(), g.toInt(), b.toInt())
            }
            speedUnit >= 55.0f -> {
                val t = (speedUnit - 55.0f) / 5.0f      // 55-60 range
                val r = lerp(255, 255, t)
                val g = lerp(215, 140, t)
                val b = lerp(0, 0, t)
                Color.rgb(r.toInt(), g.toInt(), b.toInt())
            }
            speedUnit >= 50.0f -> {
                val t = (speedUnit - 50.0f) / 5.0f      // 50-55 range
                val r = lerp(255, 255, t)
                val g = lerp(255, 215, t)
                val b = lerp(255, 0, t)
                Color.rgb(r.toInt(), g.toInt(), b.toInt())
            }
            speedUnit >= 25.0f -> {
                val t = (speedUnit - 25.0f) / 25.0f     // 25-50 range
                val r = lerp(0, 255, t)
                val g = lerp(255, 255, t)
                val b = lerp(0, 255, t)
                Color.rgb(r.toInt(), g.toInt(), b.toInt())
            }
            speedUnit >= 0.0f -> {
                val t = speedUnit / 25.0f               // 0-25 range
                val r = lerp(0, 0, t)
                val g = lerp(0, 255, t)
                val b = lerp(255, 0, t)
                Color.rgb(r.toInt(), g.toInt(), b.toInt())
            }
            else -> Color.rgb(0, 0, 255)                // Bright blue for < 0
        }
    }

    // Helper function to linearly interpolate between two values
    private fun lerp(start: Int, end: Int, fraction: Float): Float {
        return start + (end - start) * fraction.coerceIn(0f, 1f)
    }
}