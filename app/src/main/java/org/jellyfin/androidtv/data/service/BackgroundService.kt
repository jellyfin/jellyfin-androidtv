package org.jellyfin.androidtv.data.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.Size
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.MainThread
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.window.layout.WindowMetricsCalculator
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.search.SearchHint
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.serializer.toUUID
import timber.log.Timber
import java.util.concurrent.ExecutionException

class BackgroundService(
	private val context: Context,
	private val apiClient: ApiClient,
	private val userPreferences: UserPreferences,
) {
	companion object {
		const val TRANSITION_DURATION = 400L // 0.4 seconds
		const val SLIDESHOW_DURATION = 10000L // 10 seconds
		const val UPDATE_INTERVAL = 500L // 0.5 seconds
		val FRAGMENT_TAG = BackgroundServiceFragment::class.qualifiedName!!
	}

	// Async
	private val scope = MainScope()
	private var loadBackgroundsJob: Job? = null
	private var updateBackgroundTimerJob: Job? = null
	private var lastBackgroundUpdate = 0L

	// All background drawables currently showing
	internal val backgrounds = mutableListOf<Drawable>()

	// Current background index
	private var currentIndex = 0

	// Preferred display size, set when calling [attach].
	private var windowSize = Size(0, 0)
	private var windowBackground: Drawable = ColorDrawable(Color.BLACK)

	// Background layers
	internal val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.layer_background) as LayerDrawable

	// Filter to darken backgrounds
	private val colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
		ContextCompat.getColor(context, R.color.background_filter),
		BlendModeCompat.SRC_ATOP
	)

	// Animation
	@Suppress("MagicNumber")
	private val backgroundAnimator = ValueAnimator.ofInt(0, 255).apply {
		interpolator = AccelerateDecelerateInterpolator()
		duration = TRANSITION_DURATION

		addUpdateListener { animation ->
			// Set alpha
			val value = animation.animatedValue as Int
			backgroundDrawable.findDrawableByLayerId(R.id.background_next).alpha = value
			backgroundDrawable.invalidateSelf()
		}

		doOnEnd {
			// Set next as current and clear next
			val drawable = backgroundDrawable.findDrawableByLayerId(R.id.background_next)
			backgroundDrawable.setDrawableByLayerId(R.id.background_current, drawable)
			backgroundDrawable.setDrawableByLayerId(R.id.background_next, ColorDrawable(Color.TRANSPARENT))
			backgroundDrawable.invalidateSelf()
		}
	}

	/**
	 * Attach the bakground to [activity].
	 */
	fun attach(activity: FragmentActivity) {
		// Set default background to current if it's not layered
		val current = activity.window.decorView.background
		windowBackground = if (current !is LayerDrawable) current.copy() else ColorDrawable(Color.BLACK)
		backgroundDrawable.setDrawableByLayerId(R.id.background_static, windowBackground)

		// Store size of window manager for this activity
		windowSize = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity).let {
			Size(it.bounds.width(), it.bounds.height())
		}

		// Add a fragment to the activity to automatically set the background on resume
		val fragment = activity.supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
		if (fragment == null) {
			// Add fragment
			Timber.i("Adding BackgroundServiceFragment to activity")

			activity.supportFragmentManager
				.beginTransaction()
				.add<BackgroundServiceFragment>(FRAGMENT_TAG)
				.commit()
		}
	}

	// Helper function for [setBackground]
	private fun ArrayList<String>?.getUrls(itemId: String?): List<String> {
		// Check for nullability
		if (itemId == null || isNullOrEmpty()) return emptyList()

		return mapIndexed { index, tag ->
			apiClient.imageApi.getItemImageUrl(
				itemId = itemId.toUUID(),
				imageType = ImageType.BACKDROP,
				tag = tag,
				imageIndex = index,
			)
		}
	}

	/**
	 * Use all available backdrops from [baseItem] as background.
	 */
	fun setBackground(baseItem: BaseItemDto?) {
		// Check if item is set and backgrounds are enabled
		if (baseItem == null || !userPreferences[UserPreferences.backdropEnabled])
			return clearBackgrounds()

		// Get all backdrop urls
		val itemBackdropUrls = baseItem.backdropImageTags.getUrls(baseItem.id)
		val parentBackdropUrls = baseItem.parentBackdropImageTags.getUrls(baseItem.parentBackdropItemId)
		val backdropUrls = itemBackdropUrls.union(parentBackdropUrls)

		loadBackgrounds(backdropUrls)
	}

	/**
	 * Use backdrop from [searchHint] as background.
	 */
	fun setBackground(searchHint: SearchHint) {
		// Check if item is set and backgrounds are enabled
		if (!userPreferences[UserPreferences.backdropEnabled])
			return clearBackgrounds()

		// Manually grab the backdrop URL
		val backdropUrls = setOfNotNull(searchHint.backdropImageItemId?.let { itemId ->
			apiClient.imageApi.getItemImageUrl(
				itemId = itemId.toUUID(),
				imageType = ImageType.BACKDROP,
				tag = searchHint.backdropImageTag,
			)
		})

		loadBackgrounds(backdropUrls)
	}

	private fun loadBackgrounds(backdropUrls: Set<String>) {
		if (backdropUrls.isEmpty()) return clearBackgrounds()

		// Cancel current loading job
		loadBackgroundsJob?.cancel()
		loadBackgroundsJob = scope.launch(Dispatchers.IO) {
			val backdropDrawables = backdropUrls
				.map { url ->
					Glide.with(context)
						.load(url)
						.override(windowSize.width, windowSize.height)
						.centerCrop()
						.submit()
				}
				.map { future ->
					async {
						try {
							future.get()
						} catch (ex: ExecutionException) {
							Timber.e(ex, "There was an error fetching the background image.")
							null
						}
					}
				}
				.awaitAll()
				.filterNotNull()
				.onEach { it.colorFilter = colorFilter }

			backgrounds.clear()
			backgrounds.addAll(backdropDrawables)

			withContext(Dispatchers.Main) {
				// Go to first background
				currentIndex = 0
				update()
			}
		}
	}

	fun clearBackgrounds() {
		loadBackgroundsJob?.cancel()

		if (backgrounds.isEmpty()) return

		backgrounds.clear()
		update()
	}

	@MainThread
	internal fun update() {
		val now = System.currentTimeMillis()
		if (lastBackgroundUpdate > now - UPDATE_INTERVAL)
			return setTimer(lastBackgroundUpdate - now + UPDATE_INTERVAL, false)

		lastBackgroundUpdate = now

		// Snapshot the current state if an animation is running and draw the new
		// background on top.
		if (backgroundAnimator.isRunning) {
			val current = backgroundDrawable
				.toBitmap(windowSize.width, windowSize.height)
				.toDrawable(context.resources)
			backgroundAnimator.end()
			backgroundDrawable.setDrawableByLayerId(R.id.background_current, current)
		}

		// Get next background to show
		if (currentIndex >= backgrounds.size) currentIndex = 0

		backgroundDrawable.setDrawableByLayerId(
			R.id.background_next,
			backgrounds.getOrElse(currentIndex) { windowBackground.copy() }
		)

		// Animate
		backgroundAnimator.start()

		// Set timer for next background
		if (backgrounds.size > 1) setTimer()
		else updateBackgroundTimerJob?.cancel()
	}

	private fun setTimer(updateDelay: Long = SLIDESHOW_DURATION, increaseIndex: Boolean = true) {
		updateBackgroundTimerJob?.cancel()
		updateBackgroundTimerJob = scope.launch {
			delay(updateDelay)

			if (increaseIndex) currentIndex++

			update()
		}
	}

	private fun Drawable.copy() = constantState!!.newDrawable().mutate()
}
