package org.jellyfin.androidtv.ui.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

object TypographyDefaults {
	val Default: TextStyle = TextStyle.Default
}

@Immutable
data class Typography(
	val default: TextStyle = TypographyDefaults.Default,
)

val LocalTypography = staticCompositionLocalOf { Typography() }
