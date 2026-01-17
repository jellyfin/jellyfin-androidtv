package org.jellyfin.androidtv.ui.base.form

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun ColorSwatch(
	color: Color,
	modifier: Modifier = Modifier,
	shape: Shape = CircleShape,
) {
	Box(
		modifier = modifier
			.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
			.clip(shape)
	) {
		Canvas(
			modifier = Modifier
				.matchParentSize()
		) {
			if (color.alpha < 1f) {
				val squareSize = 6.dp.toPx()
				repeat((size.height / squareSize).toInt()) { row ->
					repeat((size.width / squareSize).toInt()) { col ->
						val isLight = (row + col) % 2 == 0

						drawRect(
							color = if (isLight) Color.LightGray else Color.DarkGray,
							topLeft = Offset(col * squareSize, row * squareSize),
							size = Size(squareSize, squareSize),
						)
					}
				}
			}

			drawRect(
				color = color,
			)
		}
	}
}
