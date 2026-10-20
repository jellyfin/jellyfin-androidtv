package org.jellyfin.androidtv.ui.composable

import android.app.ActivityManager
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kotlin.math.round

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
	if(url != null) {
		val isLowRamDevice = LocalContext.current.getSystemService<ActivityManager>()?.isLowRamDevice == true
		AsyncImage(
			model = url,
			contentDescription = null,
			placeholder =
				if(!isLowRamDevice && blurHash != null)
					blurHashPainter(blurHash, IntSize(
						if (aspectRatio > 1) round(blurHashResolution * aspectRatio).toInt() else blurHashResolution,
						if (aspectRatio >= 1) blurHashResolution else round(blurHashResolution / aspectRatio).toInt()))
				else null,
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
