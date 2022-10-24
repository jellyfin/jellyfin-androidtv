package org.jellyfin.androidtv.ui.composable

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.jellyfin.androidtv.ui.AsyncImageView

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
	aspectRatio: Double = 1.0,
	blurHashResolution: Int = 32,
	scaleType: ImageView.ScaleType? = null,
) {
	// Only the important properties are added to AsyncImageState
	var state by remember { mutableStateOf<AsyncImageState?>(null) }

	AndroidView(
		modifier = modifier,
		factory = { context ->
			AsyncImageView(context).also { view ->
				view.scaleType = scaleType
			}
		},
		update = { view ->
			val compositionState = AsyncImageState(url, blurHash)
			if (state != compositionState) {
				state = compositionState

				view.load(
					url = compositionState.url,
					blurHash = compositionState.blurHash,
					placeholder = placeholder,
					aspectRatio = aspectRatio,
					blurHashResolution = blurHashResolution,
				)
			}
		},
	)
}
