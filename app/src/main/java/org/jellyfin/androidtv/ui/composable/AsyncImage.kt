package org.jellyfin.androidtv.ui.composable

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import org.jellyfin.androidtv.util.BlurHashDecoder

private data class AsyncImageState(
	val url: String?,
	val blurHash: String?,
)

@Composable
fun AsyncImage(
	modifier: Modifier = Modifier,
	url: String? = null,
	blurHash: String? = null,
	placeholder: Drawable? = null,
	aspectRatio: Float = 1f,
	blurHashResolution: Int = 32,
	scaleType: ContentScale = ContentScale.Fit,
) {
	// Only the important properties are added to AsyncImageState
	var state by remember { mutableStateOf<AsyncImageState?>(null) }

	if (blurHash != null) {
		Image(
			painter = blurHashPainter(blurHash, IntSize(32, 32)),
			contentDescription = null,
			alignment = Alignment.Center,
			contentScale = ContentScale.FillBounds,
			modifier = Modifier.fillMaxSize(),
		)
	}

	AsyncImage(
		model = url,
		contentDescription = null,
		modifier = modifier,
		contentScale = scaleType,
	)
}

@Composable
fun blurHashPainter(
	blurHash: String,
	size: IntSize,
	punch: Float = 1f,
): Painter = remember(blurHash, size, punch) {
	val bitmap = BlurHashDecoder.decode(
		blurHash = blurHash,
		width = size.width,
		height = size.height,
		punch = punch,
	)

	BitmapPainter(requireNotNull(bitmap).asImageBitmap())
}
