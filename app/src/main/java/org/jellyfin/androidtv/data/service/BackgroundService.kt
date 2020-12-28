package org.jellyfin.androidtv.data.service

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.Size
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.MainThread
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import timber.log.Timber
import java.util.concurrent.ExecutionException

class BackgroundService(
	private val context: Context
) {
	// All background drawables currently showing
	private val backgrounds = mutableListOf<Drawable>()

	// Current background index
	private var currentIndex = -1

	// Prefered display size, set when calling [attach].
	private var windowSize = Size(0, 0)
	private var windowBackground: Drawable = ColorDrawable(Color.BLACK)

	// Background layers
	private val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.layer_background) as LayerDrawable
	private val staticBackgroundLayer = backgroundDrawable.findIndexByLayerId(R.id.background_static)
	private val currentBackgroundLayer = backgroundDrawable.findIndexByLayerId(R.id.background_current)
	private val nextBackgroundLayer = backgroundDrawable.findIndexByLayerId(R.id.background_next)

	// Animation
	private val backgroundAnimator = ValueAnimator.ofInt(0, 255).apply {
		interpolator = AccelerateDecelerateInterpolator()
		duration = 400L // 0.4 seconds

		addUpdateListener { animation ->
			// Set alpha
			val value = animation.animatedValue as Int
			backgroundDrawable.getDrawable(nextBackgroundLayer).alpha = value
			backgroundDrawable.invalidateSelf()
		}

		doOnEnd {
			// Set next as current and clear next
			val drawable = backgroundDrawable.getDrawable(nextBackgroundLayer)
			backgroundDrawable.setDrawable(currentBackgroundLayer, drawable)
			backgroundDrawable.setDrawable(nextBackgroundLayer, ColorDrawable(Color.TRANSPARENT))
			backgroundDrawable.invalidateSelf()
		}
	}

	/**
	 * Attach the bakground to [activity].
	 */
	fun attach(activity: Activity) {
		// Set default background to current
		val current = activity.window.decorView.background
		windowBackground = current?.copy() ?: ColorDrawable(Color.BLACK)
		backgroundDrawable.setDrawable(staticBackgroundLayer, windowBackground)

		// Store size of window manager for this activity
		windowSize = Point()
			.apply { activity.window.windowManager.defaultDisplay.getSize(this) }
			.let { Size(it.x, it.y) }

		// Replace current background with service background
		activity.window.decorView.background = backgroundDrawable

		// Update
		update()
	}

	fun setBackground(bitmap: Bitmap) = GlobalScope.launch(Dispatchers.IO) {
		val drawable = Glide.with(context)
			.load(bitmap)
			.override(windowSize.width, windowSize.height)
			.centerCrop()
			.submit()
			.get()

		backgrounds.clear()
		backgrounds += drawable

		withContext(Dispatchers.Main) {
			update()
		}
	}

	// TODO: add set methods for baseitem or list of urls
	//TODO suspend?
	fun setBackground(url: String?) = GlobalScope.launch(Dispatchers.IO) {
		Timber.i("Set background to %s", url)

		//TODO get null
		val drawable = try {
			Glide.with(context)
				.load(url)
				.override(windowSize.width, windowSize.height)
				.centerCrop()
				.submit()
				.get()
		} catch (err: ExecutionException) {
			Timber.e(err)

			null
		}

		backgrounds.clear()
		if (drawable != null) backgrounds += drawable

		withContext(Dispatchers.Main) {
			update()
		}
	}

	fun clearBackgrounds() {
		backgrounds.clear()
		update()
	}

	@MainThread
	private fun update() {
		// Snapshot the current state if an animation is running and draw the new
		// background on top.
		if (backgroundAnimator.isRunning) {
			val current = backgroundDrawable
				.toBitmap(windowSize.width, windowSize.height)
				.toDrawable(context.resources)
			backgroundAnimator.end()
			backgroundDrawable.setDrawable(currentBackgroundLayer, current)
		}

		// Get next background to show
		currentIndex++
		if (currentIndex >= backgrounds.size) currentIndex = 0

		backgroundDrawable.setDrawable(
			nextBackgroundLayer,
			backgrounds.getOrElse(currentIndex) { windowBackground.copy() }
		)

		// Animate
		backgroundAnimator.start()
	}

	private fun Drawable.copy() = constantState!!.newDrawable().mutate()
}
