package org.jellyfin.androidtv.integration.dream

import android.graphics.drawable.Drawable
import android.service.dreams.DreamService
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.databinding.DreamLibraryBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.apiclient.model.querying.ItemSortBy
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageFormat
import org.jellyfin.sdk.model.api.ImageType
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.ExecutionException

/**
 * An Android [DreamService] (screensaver) that shows TV series and movies from all libraries.
 * Use `adb shell am start -n "com.android.systemui/.Somnambulator"` to start after changing the
 * default screensaver in the device settings.
 */
class LibraryDreamService : DreamService() {
	companion object {
		const val INITIAL_DELAY = 2_000L // 2s
		const val UPDATE_DELAY = 30_000L // 30s
		const val TRANSITION_DURATION = 1_000L // 1s
	}

	private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
	private val api: ApiClient by inject()
	private val userPreferences: UserPreferences by inject()

	private lateinit var binding: DreamLibraryBinding

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()

		isInteractive = false
		isFullscreen = true

		binding = DreamLibraryBinding.inflate(LayoutInflater.from(window.context), null, false)
		binding.clock.isVisible = when (userPreferences[UserPreferences.clockBehavior]) {
			ClockBehavior.ALWAYS, ClockBehavior.IN_MENUS -> true
			else -> false
		}

		setContentView(binding.root)
	}

	override fun onDreamingStarted() {
		super.onDreamingStarted()

		scope.launch {
			delay(INITIAL_DELAY)

			while (isActive) {
				update()
				delay(UPDATE_DELAY)
			}
		}
	}

	private suspend fun getRandomItem(): BaseItemDto? = try {
		val response by api.itemsApi.getItemsByUserId(
			includeItemTypes = listOf("Movie", "Series"),
			recursive = true,
			sortBy = listOf(ItemSortBy.Random),
			limit = 1,
			imageTypes = listOf(ImageType.BACKDROP)
		)

		response.items?.firstOrNull()
	} catch (err: ApiClientException) {
		Timber.e(err)
		null
	}

	private suspend fun loadBackdrop(item: BaseItemDto): Drawable? {
		val tag = item.backdropImageTags?.randomOrNull()
			?: item.imageTags?.get(ImageType.BACKDROP)

		val url = api.imageApi.getItemImageUrl(
			itemId = item.id,
			imageType = ImageType.BACKDROP,
			tag = tag,
			format = ImageFormat.WEBP,
		)

		return withContext(Dispatchers.IO) {
			// Sometimes the backdrop doesn't exist
			// make sure to return a null value in those cases
			try {
				Glide.with(binding.root)
					.load(url)
					.submit()
					.get()
			} catch (err: ExecutionException) {
				Timber.e(err)
				null
			}
		}
	}

	private suspend fun update() {
		val item = getRandomItem()
		val background = item?.let { loadBackdrop(it) }

		withContext(Dispatchers.Main) {
			if (item == null || background == null) {
				binding.itemSwitcher.hideAllViews()
			} else with(binding.itemSwitcher) {
				getNextView<LibraryDreamItemView>().setItem(
					item = LibraryDreamItem(
						baseItem = item,
						background = background,
					)
				)
				showNextView()
			}
		}
	}

	override fun onDreamingStopped() {
		super.onDreamingStopped()

		scope.cancel()
	}
}
