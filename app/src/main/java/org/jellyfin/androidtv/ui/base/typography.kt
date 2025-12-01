package org.jellyfin.androidtv.ui.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

object TypographyDefaults {
	val Default: TextStyle = TextStyle.Default

	val ListHeader: TextStyle = Default.copy(
		fontSize = 15.sp,
		lineHeight = 20.sp,
		fontWeight = FontWeight.W700,
	)
	val ListOverline: TextStyle = Default.copy(
		fontSize = 10.sp,
		lineHeight = 12.sp,
		fontWeight = FontWeight.W600,
		letterSpacing = 0.65.sp,
	)
	val ListHeadline: TextStyle = Default.copy(
		fontSize = 14.sp,
		lineHeight = 28.sp,
		fontWeight = FontWeight.W600,
	)
	val ListCaption: TextStyle = Default.copy(
		fontSize = 11.sp,
		lineHeight = 14.sp,
		fontWeight = FontWeight.W500,
		letterSpacing = 0.1.sp,
	)

	val Badge: TextStyle = Default.copy(
		fontSize = 11.sp,
		fontWeight = FontWeight.W700,
		textAlign = TextAlign.Center,
	)
}

@Immutable
data class Typography(
	val default: TextStyle = TypographyDefaults.Default,
	val listHeader: TextStyle = TypographyDefaults.ListHeader,
	val listOverline: TextStyle = TypographyDefaults.ListOverline,
	val listHeadline: TextStyle = TypographyDefaults.ListHeadline,
	val listCaption: TextStyle = TypographyDefaults.ListCaption,
	val badge: TextStyle = TypographyDefaults.Badge
)

val LocalTypography = staticCompositionLocalOf { Typography() }
