package org.jellyfin.androidtv.data.service

import android.content.Context
import androidx.annotation.MainThread
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.SearchHint
import org.jellyfin.sdk.model.serializer.toUUID
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ExecutionException

class BackgroundService(
	private val context: Context,
	private val jellyfin: Jellyfin,
	private val api: ApiClient,
	private val userPreferences: UserPreferences,
) {
	companion object {
		const val SLIDESHOW_DURATION = 30000L // 30 seconds
		const val UPDATE_INTERVAL = 500L // 0.5 seconds
	}

	// Async
	private val scope = MainScope()
	private var loadBackgroundsJob: Job? = null
	private var updateBackgroundTimerJob: Job? = null
	private var lastBackgroundUpdate = 0L

	// All background drawables currently showing
	private val _backgrounds = MutableStateFlow(emptyList<ImageBitmap>())
	val backgrounds get() = _backgrounds.asStateFlow()

	// Current background index
	private var _currentIndex = MutableStateFlow(0)
	val currentIndex get() = _currentIndex.asStateFlow()

	// Helper function for [setBackground]
	private fun List<String>?.getUrls(itemId: UUID?): List<String> {
		// Check for nullability
		if (itemId == null || isNullOrEmpty()) return emptyList()

		return mapIndexed { index, tag ->
			api.imageApi.getItemImageUrl(
				itemId = itemId,
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
			api.imageApi.getItemImageUrl(
				itemId = itemId.toUUID(),
				imageType = ImageType.BACKDROP,
				tag = searchHint.backdropImageTag,
			)
		})

		loadBackgrounds(backdropUrls)
	}

	/**
	 * Use splashscreen from [server] as background.
	 */
	fun setBackground(server: Server) {
		// Check if item is set and backgrounds are enabled
		if (!userPreferences[UserPreferences.backdropEnabled])
			return clearBackgrounds()

		// Check if splashscreen is enabled in (cached) branding options
		if (!server.splashscreenEnabled)
			return clearBackgrounds()

		// Manually grab the backdrop URL
		val api = jellyfin.createApi(baseUrl = server.address)
		val splashscreenUrl = api.imageApi.getSplashscreenUrl()

		loadBackgrounds(setOf(splashscreenUrl))
	}


	private fun loadBackgrounds(backdropUrls: Set<String>) {
		if (backdropUrls.isEmpty()) return clearBackgrounds()

		// Cancel current loading job
		loadBackgroundsJob?.cancel()
		loadBackgroundsJob = scope.launch(Dispatchers.IO) {
			_backgrounds.value = backdropUrls
				.map { url ->
					Glide.with(context).asBitmap().load(url).submit()
				}
				.map { future ->
					async {
						try {
							future.get().asImageBitmap()
						} catch (ex: ExecutionException) {
							Timber.e(ex, "There was an error fetching the background image.")
							null
						}
					}
				}
				.awaitAll()
				.filterNotNull()

			withContext(Dispatchers.Main) {
				// Go to first background
				_currentIndex.value = 0
				update()
			}
		}
	}

	fun clearBackgrounds() {
		loadBackgroundsJob?.cancel()

		if (_backgrounds.value.isEmpty()) return

		_backgrounds.value = emptyList()
		update()
	}

	@MainThread
	internal fun update() {
		val now = System.currentTimeMillis()
		if (lastBackgroundUpdate > now - UPDATE_INTERVAL)
			return setTimer(lastBackgroundUpdate - now + UPDATE_INTERVAL, false)

		lastBackgroundUpdate = now

		// Get next background to show
		if (_currentIndex.value >= _backgrounds.value.size) _currentIndex.value = 0

		// Set timer for next background
		if (_backgrounds.value.size > 1) setTimer()
		else updateBackgroundTimerJob?.cancel()
	}

	private fun setTimer(updateDelay: Long = SLIDESHOW_DURATION, increaseIndex: Boolean = true) {
		updateBackgroundTimerJob?.cancel()
		updateBackgroundTimerJob = scope.launch {
			delay(updateDelay)

			if (increaseIndex) _currentIndex.value++

			update()
		}
	}
}
