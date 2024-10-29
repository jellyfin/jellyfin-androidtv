package org.jellyfin.androidtv.ui.composable.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.fadingEdges(
	all: Dp
) = fadingEdges(
	start = all,
	top = all,
	end = all,
	bottom = all,
)

fun Modifier.fadingEdges(
	vertical: Dp = 0.dp,
	horizontal: Dp = 0.dp,
) = fadingEdges(
	start = horizontal,
	top = vertical,
	end = horizontal,
	bottom = vertical,
)

fun Modifier.fadingEdges(
	start: Dp = 0.dp,
	top: Dp = 0.dp,
	end: Dp = 0.dp,
	bottom: Dp = 0.dp,
) = then(
	Modifier
		.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
		.drawWithContent {
			drawContent()

			// Start edge
			if (start.value > 0) {
				drawRect(
					brush = Brush.horizontalGradient(
						0f to Color.Transparent,
						1f to Color.Black,
						endX = start.toPx(),
					),
					blendMode = BlendMode.DstIn,
				)
			}

			// Top edge
			if (top.value > 0) {
				drawRect(
					brush = Brush.verticalGradient(
						0f to Color.Transparent,
						1f to Color.Black,
						endY = top.toPx(),
					),
					blendMode = BlendMode.DstIn,
				)
			}

			// End edge
			if (end.value > 0) {
				drawRect(
					brush = Brush.horizontalGradient(
						0f to Color.Black,
						1f to Color.Transparent,
						startX = size.width - end.toPx(),
					),
					blendMode = BlendMode.DstIn,
				)
			}

			// Bottom edge
			if (bottom.value > 0) {
				drawRect(
					brush = Brush.verticalGradient(
						0f to Color.Black,
						1f to Color.Transparent,
						startY = size.height - bottom.toPx(),
					),
					blendMode = BlendMode.DstIn
				)
			}
		}
)

@Preview
@Composable
private fun FadingEdgePreview() {
	Box(
		modifier = Modifier
			.size(100.dp)
			.background(Color.Red)
	) {
		Box(
			modifier = Modifier
				.size(50.dp)
				.fadingEdges(10.dp, 0.dp, 25.dp, 3.dp)
				.background(Color.Blue)
				.align(Alignment.Center)
		) { }
	}
}
