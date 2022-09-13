package org.jellyfin.androidtv.constant

import android.content.Context
import org.jellyfin.androidtv.R

@Suppress("MagicNumber")
private val qualityOptions = setOf(
	0.0, // auto
	120.0, 110.0, 100.0, // 100 >=
	90.0, 80.0, 70.0, 60.0, 50.0, 40.0, 30.0, 20.0, 15.0, 10.0, // 10 >=
	5.0, 3.0, 2.0, 1.0, // 1 >=
	0.72, 0.42 // 0 >=
)

@Suppress("MagicNumber")
fun qualityProfiles(context: Context): Map<String,String> {
	val strings = qualityOptions.associate {
		val value = when {
			it == 0.0 -> context.getString(R.string.bitrate_auto)
			it >= 1.0 -> context.getString(R.string.bitrate_mbit, it)
			else -> context.getString(R.string.bitrate_kbit, it * 1000.0)
		}

		it.toString().removeSuffix(".0") to value
	}
	return strings
}

