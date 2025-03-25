package org.jellyfin.androidtv.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toolingGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun Icon(
	imageVector: ImageVector,
	contentDescription: String?,
	modifier: Modifier = Modifier,
	tint: Color = LocalTextStyle.current.color,
) = Icon(
	painter = rememberVectorPainter(imageVector),
	contentDescription = contentDescription,
	modifier = modifier,
	tint = tint,
)

@Composable
fun Icon(
	bitmap: ImageBitmap,
	contentDescription: String?,
	modifier: Modifier = Modifier,
	tint: Color = LocalTextStyle.current.color,
) = Icon(
	painter = remember(bitmap) { BitmapPainter(bitmap) },
	contentDescription = contentDescription,
	modifier = modifier,
	tint = tint,
)

@Composable
fun Icon(
	painter: Painter,
	contentDescription: String?,
	modifier: Modifier = Modifier,
	tint: Color = LocalTextStyle.current.color,
) {
	val colorFilter = remember(tint) { tint.takeUnless { it == Color.Unspecified }?.let(ColorFilter::tint) }
	val semantics = if (contentDescription != null) Modifier.semantics {
		this.contentDescription = contentDescription
		this.role = Role.Image
	} else Modifier

	Box(
		modifier
			.toolingGraphicsLayer()
			.defaultSizeFor(painter)
			.paint(painter, colorFilter = colorFilter, contentScale = ContentScale.Fit)
			.then(semantics)
	)
}

private fun Modifier.defaultSizeFor(painter: Painter) = then(
	if (painter.intrinsicSize == Size.Unspecified || painter.intrinsicSize.isInfinite()) {
		Modifier.size(20.dp)
	} else {
		Modifier
	}
)

private fun Size.isInfinite() = width.isInfinite() && height.isInfinite()

