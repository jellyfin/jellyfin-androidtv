package org.jellyfin.androidtv.ui.playback.overlay.action

import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration

fun formatSubtitleOffsetSeconds(offsetUs: Long): String {
	val seconds = offsetUs / 1_000_000.0
	val safeSeconds = if (abs(seconds) < 0.05) 0.0 else seconds
	return String.format(Locale.getDefault(), "%+.1f", safeSeconds)
}

fun formatSubtitleOffsetSeconds(offset: Duration): String =
	formatSubtitleOffsetSeconds(offset.inWholeMicroseconds)
