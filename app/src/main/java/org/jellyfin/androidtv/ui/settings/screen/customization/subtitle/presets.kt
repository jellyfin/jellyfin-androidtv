package org.jellyfin.androidtv.ui.settings.screen.customization.subtitle

import androidx.compose.ui.graphics.Color

private val SubtitleColorPresets = setOf(
	Color(0xFFFFFFFFL),
	Color(0XFF000000L),
	Color(0xFF7F7F7FL),
	Color(0xFFC80000L),
	Color(0xFF00C800L),
	Color(0xFF0000C8L),
	Color(0xFFEEDC00L),
	Color(0xFFD60080L),
	Color(0xFF009FDAL),
)

val SubtitleTextColorPresets = SubtitleColorPresets
val SubtitleBackgroundColorPresets = setOf(Color.Transparent) + SubtitleColorPresets
val SubtitleTextStrokeColorPresets = setOf(Color.Transparent) + SubtitleColorPresets
