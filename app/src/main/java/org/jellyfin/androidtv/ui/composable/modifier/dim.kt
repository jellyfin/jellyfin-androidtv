package org.jellyfin.androidtv.ui.composable.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.dim(dim: Boolean = true) = then(
	Modifier
		.graphicsLayer(
			colorFilter = ColorFilter.tint(
				Color(
					0.0f, 0.0f, 0.0f,
					if (dim) 0.4f else 0.0f
				),
				blendMode = BlendMode.SrcAtop
			)
		)
)
