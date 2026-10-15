package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** OKLCH → sRGB для Compose. Те же значения, что в CSS: oklch(L C H / alpha). */
fun oklch(l: Double, c: Double, hDeg: Double, alpha: Float = 1f): Color {
    val h = Math.toRadians(hDeg)
    val a = c * cos(h)
    val b = c * sin(h)
    val l1 = l + 0.3963377774 * a + 0.2158037573 * b
    val m1 = l - 0.1055613458 * a - 0.0638541728 * b
    val s1 = l - 0.0894841775 * a - 1.2914855480 * b
    val lc = l1 * l1 * l1
    val mc = m1 * m1 * m1
    val sc = s1 * s1 * s1
    val r = 4.0767416621 * lc - 3.3077115913 * mc + 0.2309699292 * sc
    val g = -1.2684380046 * lc + 2.6097574011 * mc - 0.3413193965 * sc
    val bl = -0.0041960863 * lc - 0.7034186147 * mc + 1.7076147010 * sc
    return Color(gamma(r), gamma(g), gamma(bl), alpha)
}

private fun gamma(x: Double): Float {
    val c = x.coerceIn(0.0, 1.0)
    val v = if (c >= 0.0031308) 1.055 * c.pow(1.0 / 2.4) - 0.055 else 12.92 * c
    return v.toFloat()
}
