package org.jellyfin.androidtv.util

import android.util.Log
import okhttp3.internal.toHexString
import timber.log.Timber
import kotlin.math.roundToLong

/**
 * Adds an opacity to a Long color.
 * Conversion to java.lang.Long is required for usage in VideoManager.java
 * @param opacity Opacity as a float in range 0..1
 * @return `this` color with opacity
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Long.withOpacity(opacity: Float): java.lang.Long {
	if (opacity > 1f) {
		throw IllegalArgumentException("Opacity must be lower or equal to 1")
	}

	val op = ((opacity * 255).roundToLong() shl 24) or 0xFFFFFF

	Timber.d((this and op).toHexString())

	return (this and op) as java.lang.Long
}
