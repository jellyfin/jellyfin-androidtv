package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object ButtonDefaults {
	val Shape: Shape = CircleShape
	val ContentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
}
