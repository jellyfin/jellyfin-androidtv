package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.doOnAttach
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.util.BlurHashDecoder
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds

/**
 * An extension to the [ImageView] that makes it easy to load images from the network.
 * The [load] function takes a url, blurhash and placeholder to asynchronously load the image
 * using the lifecycle of the current fragment or activity.
 */
class AsyncImageView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {
	private val lifeCycleOwner get() = findViewTreeLifecycleOwner()

	/**
	 * The duration of the crossfade when changing switching the images of the url, blurhash and
	 * placeholder.
	 */
	var crossFadeDuration = 100.milliseconds

	/**
	 * Load an image from the network using [url]. When the [url] is null or returns a bad response
	 * the [placeholder] is shown. A [blurHash] is shown while loading the image. An aspect ratio is
	 * required when using a BlurHash or the sizing will be incorrect.
	 */
	fun load(
		url: String? = null,
		blurHash: String? = null,
		placeholder: Drawable? = null,
		aspectRatio: Double = 1.0,
		blurHashResolution: Int = 32,
	) = doOnAttach {
		lifeCycleOwner?.lifecycleScope?.launch {
			var placeholderOrBlurHash = placeholder

			// Only show blurhash if an image is going to be loaded from the network
			if (url != null && blurHash != null) withContext(Dispatchers.IO) {
				val blurHashBitmap = BlurHashDecoder.decode(
					blurHash,
					if (aspectRatio > 1) round(blurHashResolution * aspectRatio).toInt() else blurHashResolution,
					if (aspectRatio >= 1) blurHashResolution else round(blurHashResolution / aspectRatio).toInt(),
				)
				if (blurHashBitmap != null) placeholderOrBlurHash = blurHashBitmap.toDrawable(resources)
			}

			// Start loading image or placeholder
			Glide.with(this@AsyncImageView)
				.load(url ?: placeholder)
				.placeholder(placeholderOrBlurHash)
				.error(placeholder)
				// FIXME: Glide is unable to scale the image when transitions are enabled
				//.transition(DrawableTransitionOptions.withCrossFade(crossFadeDuration.inWholeMilliseconds.toInt()))
				.into(this@AsyncImageView)
		}
	}
}
