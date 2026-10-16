package org.jellyfin.androidtv.ui.composable

import android.app.ActivityManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.core.content.getSystemService
import coil3.compose.AsyncImage
import org.jellyfin.androidtv.util.BlurHashDecoder

@Composable
fun AsyncImage(
	modifier: Modifier = Modifier,
	url: String? = null,
	blurHash: String? = null,
	placeholder: Int? = null,
	aspectRatio: Float = 1f,
	blurHashResolution: Int = 32,
	scaleType: ContentScale = ContentScale.Fit,
) {
	val isLowRamDevice = LocalContext.current.getSystemService<ActivityManager>()?.isLowRamDevice == true
	if (!isLowRamDevice && blurHash != null && placeholder == null) {
		Image(
			painter = blurHashPainter(blurHash, IntSize(blurHashResolution, blurHashResolution)),
			contentDescription = null,
			alignment = Alignment.Center,
			contentScale = ContentScale.FillBounds,
			modifier = Modifier.fillMaxSize(),
		)
	}

    if(url != null) {
		AsyncImage(
			model = url,
			contentDescription = null,
			modifier = modifier,
			contentScale = scaleType,
		)
	}
	else if (placeholder != null) {
	    Image(
			painter = painterResource(placeholder),
			contentDescription = null,
			modifier = modifier,
			contentScale = scaleType,
		)
	}
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
