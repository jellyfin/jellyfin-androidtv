package org.jellyfin.androidtv.ui.base.button

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ButtonColors(
	val containerColor: Color,
	val contentColor: Color,
	val focusedContainerColor: Color,
	val focusedContentColor: Color,
	val disabledContainerColor: Color,
	val disabledContentColor: Color,
)
