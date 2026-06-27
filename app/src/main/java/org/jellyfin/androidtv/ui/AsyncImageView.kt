package org.jellyfin.androidtv.ui

import android.app.ActivityManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.doOnAttach
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.asImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.target
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.BlurHashDecoder
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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
) : AppCompatImageView(context, attrs, defStyleAttr), KoinComponent {
	private val lifeCycleOwner get() = findViewTreeLifecycleOwner()
	private val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.AsyncImageView, defStyleAttr, 0)
	private val imageLoader by inject<ImageLoader>()
	private var loadJob: Job? = null

	/**
	 * The duration of the crossfade when changing switching the images of the url, blurhash and
	 * placeholder.
	 */
	@Suppress("MagicNumber")
	var crossFadeDuration = styledAttributes.getInt(R.styleable.AsyncImageView_crossfadeDuration, 100).milliseconds

	/**
	 * Shape the image to a circle and remove all corners.
	 */
	var circleCrop = styledAttributes.getBoolean(R.styleable.AsyncImageView_circleCrop, false)
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
		// Cancel the previous load if still running
		loadJob?.cancel()

		loadJob = lifeCycleOwner?.lifecycleScope?.launch(Dispatchers.IO) {
			var placeholderOrBlurHash = placeholder

			// Only show blurhash if an image is going to be loaded from the network
			val isLowRamDevice = context.getSystemService<ActivityManager>()?.isLowRamDevice == true
			if (url != null && blurHash != null && !isLowRamDevice && aspectRatio > 0) withContext(Dispatchers.IO) {
				val blurHashBitmap = BlurHashDecoder.decode(
					blurHash,
					if (aspectRatio > 1) round(blurHashResolution * aspectRatio).toInt() else blurHashResolution,
					if (aspectRatio >= 1) blurHashResolution else round(blurHashResolution / aspectRatio).toInt(),
				)
				if (blurHashBitmap != null) placeholderOrBlurHash = blurHashBitmap.toDrawable(resources)
			}

			// Start loading image or placeholder
			val request = if (url == null) {
				ImageRequest.Builder(context).apply {
					target(this@AsyncImageView)
					data(placeholder)
					if (circleCrop) transformations(CircleCropTransformation())
				}.build()
			} else {
				ImageRequest.Builder(context).apply {
					val crossFadeDurationMs = crossFadeDuration.inWholeMilliseconds.toInt()
					if (crossFadeDurationMs > 0) crossfade(crossFadeDurationMs)
					else crossfade(false)

					target(this@AsyncImageView)
					data(url)
					placeholder(placeholderOrBlurHash?.asImage())
					if (circleCrop) transformations(CircleCropTransformation())
					error(placeholder?.asImage())
				}.build()
			}

			imageLoader.enqueue(request).job.await()
		}
	}

	fun animateDocumentaryZoomPan(duration: Long?, item: BaseItemDto?, screenWidth: Int?, screenHeight: Int?){
		if(duration == null || item == null || screenWidth == null || screenHeight == null || item.width == null || item.height == null)
			return
		val imageAspectRatio = item.width!!.toDouble() / item.height!!.toDouble()
		if(imageAspectRatio > 0.56 && imageAspectRatio < 2.0) {
			val xLimit = ((item.width ?: screenWidth) * 0.25).toInt()
			val yLimit = ((item.height ?: screenHeight) * 0.25).toInt()
			val zoomEffectPercent = 0.3f
			val panEffectPercent = 0.3f
			fun getRandomScaleValue(): Float {
				return ((50..200).random().toFloat() / 100.0f * zoomEffectPercent) + 1.0f
			}
			fun getRandomPanValue(limit: Int, ): Float {
				return (-1 * limit..limit).random().toFloat() * panEffectPercent
			}
			fun getRandomSubDuration(duration: Long) : Long{
				return (duration * ((20..80).random().toFloat() / 100.0f)).toLong()
			}
			val rndX = getRandomPanValue(xLimit)
			val rndY = getRandomPanValue(yLimit)
			val rndScale = getRandomScaleValue()
			val rndScaleModifier = floatArrayOf(.85f, .90f, .95f, 1f, 1f, 1f, 1f, 1.05f, 1.10f, 1.15f).random()
			var rndX2 = rndX
			var rndY2 = rndY
			var rndScale2 = rndScale
			val actionDuration = (duration * 0.98f - crossFadeDuration.inWholeMilliseconds).toLong()
			var phase1Duration = actionDuration
			var phase2Duration = actionDuration
			var randomAction = 0
			val onlyUse1PhaseActions = duration <= 5000
			val canUseZoomOutActions = true
			val randomActionLowerLimit = 0
			val randomActionUpperLimit1Phase = 2
			val randomActionUpperLimit1PhaseExcludingZoomOut = 1
			val randomActionUpperLimit2PhaseExcludingZoomOut = 8
			var randomActionUpperLimit = 13

			if(onlyUse1PhaseActions) {
				randomActionUpperLimit = if (canUseZoomOutActions) randomActionUpperLimit1Phase else randomActionUpperLimit1PhaseExcludingZoomOut
				randomAction =(randomActionLowerLimit .. randomActionUpperLimit).random()
			}
			else {
				randomActionUpperLimit = if (canUseZoomOutActions) randomActionUpperLimit else randomActionUpperLimit2PhaseExcludingZoomOut
				randomAction =(randomActionLowerLimit .. randomActionUpperLimit).random()
				phase1Duration = getRandomSubDuration(actionDuration)
				phase2Duration =  actionDuration - phase1Duration
				if(randomAction in 8..9) {
					rndX2 = getRandomPanValue(xLimit)
					rndY2 = getRandomPanValue(yLimit)
					rndScale2 = getRandomScaleValue()
				}
			}

			if(!canUseZoomOutActions && randomAction == 2)
				randomAction = 1

			when (randomAction) {
				1 -> {
					//Zoom In
					zoomAndPanRunnable(0,0, 1.0f, 0.0f, 0.0f,actionDuration, rndScale, rndX, rndY, false, 0)
				}
				2 -> {
					//Zoom Out
					zoomAndPanRunnable(0,0, rndScale, rndX, rndY, actionDuration, 1.0f, 0.0f, 0.0f, false,0)
				}
				3 -> {
					//Zoom In and Pan to Center
					zoomAndPanRunnable(0,phase1Duration, rndScale, rndX, rndY, phase2Duration, (rndScale * rndScaleModifier), 0.0f, 0.0f, false,0)
				}
				4 -> {
					//Zoom In and Pan opposite XY
					zoomAndPanRunnable(0,phase1Duration, rndScale, rndX, rndY, phase2Duration, (rndScale * rndScaleModifier), -1 * rndX, -1 * rndY, false,0)
				}
				5 -> {
					//Zoom In and Pan opposite X
					zoomAndPanRunnable(0,phase1Duration, rndScale, rndX, rndY, phase2Duration, (rndScale * rndScaleModifier), -1 * rndX, rndY, false,0)
				}
				6 -> {
					//Zoom In and Pan opposite Y
					zoomAndPanRunnable(0,phase1Duration, rndScale, rndX, rndY, phase2Duration, (rndScale * rndScaleModifier), rndX, -1 * rndY, false,0)
				}
				7 -> {
					//Zoom In and hold
					zoomAndPanRunnable(0,phase1Duration, rndScale, rndX, rndY, phase2Duration, rndScale, rndX, rndY, false,0)
				}
				8 -> {
					//Zoom In and to new Random
					zoomAndPanRunnable(0,phase1Duration, rndScale, rndX, rndY, phase2Duration, rndScale2, rndX2, rndY2, false,0)
				}
				9 -> {
					//Zoomed In To New Random, if slide animation is 100 or less, and thumbnails are being used below 10 second interval
					zoomAndPanRunnable(0,0, rndScale, rndX, rndY, actionDuration, rndScale2, rndX2, rndY2, false,0)
				}
				10 -> {
					//Zoomed In Pan to Center
					zoomAndPanRunnable(0,0, rndScale, rndX, rndY, actionDuration, (rndScale * rndScaleModifier), 0.0f, 0.0f, false,0)
				}
				11 -> {
					//Zoomed In Pan opposite XY
					zoomAndPanRunnable(0,0, rndScale, rndX, rndY, actionDuration, (rndScale * rndScaleModifier), -1 * rndX, -1 * rndY, false,0)
				}
				12 -> {
					//Zoomed In Pan opposite X
					zoomAndPanRunnable(0,0, rndScale, rndX, rndY, actionDuration, (rndScale * rndScaleModifier), -1 * rndX, rndY, false,0)
				}
				13 -> {
					//Zoomed In Pan opposite Y
					zoomAndPanRunnable(0,0, rndScale, rndX, rndY, actionDuration, (rndScale * rndScaleModifier), rndX, -1 * rndY, false,0)
				}
			}
		}
		else if (duration >= 5000 && (imageAspectRatio <= 0.56 || imageAspectRatio >= 2.0)){
			val scaleUp = getScaleUpToViewBounds(imageAspectRatio,screenWidth,screenHeight)
			if (scaleUp > 0.9f && scaleUp < 1.1f)
				return

			val xZoomPan = (screenWidth.toFloat() * scaleUp * 0.5f) - (screenWidth.toFloat() * 0.5f)
			val yZoomPan = (screenHeight.toFloat() * scaleUp * 0.5f) - (screenHeight.toFloat() * 0.5f)
			val holdOrZoomDuration = ((duration - crossFadeDuration.inWholeMilliseconds).toFloat() * 0.03f).toLong()
			val scrollDuration = ((duration - crossFadeDuration.inWholeMilliseconds).toFloat() * 0.8f).toLong()
			val randomizeDirection = floatArrayOf(-1f,1f).random()
			if (imageAspectRatio > 2.0f) {
				zoomAndPanRunnable(holdOrZoomDuration, holdOrZoomDuration, scaleUp, randomizeDirection*xZoomPan, 0.0f, scrollDuration, scaleUp,-1*randomizeDirection*xZoomPan,0.0f, true, holdOrZoomDuration)
			}
			else if (imageAspectRatio <= 0.56f) {
				zoomAndPanRunnable(holdOrZoomDuration,holdOrZoomDuration, scaleUp, 0.0f, -1*randomizeDirection*yZoomPan, scrollDuration, scaleUp,0.0f, randomizeDirection*yZoomPan, true, holdOrZoomDuration)
			}
		}
	}
	private fun getScaleUpToViewBounds(imageAspectRatio: Double, screenWidth: Int, screenHeight: Int): Float {
		var scaleUp = 1.0f
		val screenAspectRatio = screenWidth.toDouble() / screenHeight.toDouble()
		if(imageAspectRatio >= 2.0){
			scaleUp = (imageAspectRatio / screenAspectRatio).toFloat()
		}
		else if (imageAspectRatio <= 0.56){
			scaleUp = (screenAspectRatio / imageAspectRatio).toFloat()
		}
		return scaleUp
	}
	fun zoomAndPanRunnable(startDelay: Long, duration1: Long, scale1: Float, x1: Float, y1: Float, duration2: Long, scale2: Float, x2: Float, y2: Float, resetZoomAndPan: Boolean, resetDuration: Long) {
		val resetZoomAndPanAction = Runnable{
			this.animate()
				.setDuration(resetDuration)
				.scaleY(1.0f).scaleX(1.0f)
				.x(0.0f).y(0.0f)
		}
		val panAction = Runnable {
			this.animate()
				.setDuration(duration2)
				.scaleY(scale2).scaleX(scale2)
				.x(x2).y(y2)
				.withEndAction(if(resetZoomAndPan) resetZoomAndPanAction else null)
		}
		this.animate()
			.setStartDelay(startDelay)
			.setDuration(duration1)
			.scaleY(scale1).scaleX(scale1)
			.x(x1).y(y1)
			.withEndAction(panAction)
	}
}
