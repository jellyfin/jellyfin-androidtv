package org.jellyfin.androidtv.util

import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Adds an opacity to a Long color.
 * @param opacity Opacity as a float in range 0..1
 * @return `this` color with opacity
 */
fun Long.withOpacity(opacity: Float): Long {
	if (opacity > 1f) {
		throw IllegalArgumentException("Opacity must be lower or equal to 1")
	}

	val op = ((opacity * 255f).roundToLong() shl 24)

	return ((this and 0x00FFFFFF) or op)
}

/**
 * Extract opacity from a color and applies it to color `this`.
 * @param color Color providing the opacity
 * @return `this` with the opacity of `color`
 */
fun Long.withOpacity(color: Long): Long = this.withOpacity(min(color.getOpacity(), 0.2f))

fun Long.getOpacity(): Float = (this shr 24) / 255f
