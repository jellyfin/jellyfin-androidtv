package org.jellyfin.androidtv.integration.dream

import android.service.dreams.DreamService
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.integration.dream.composable.DreamView
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageFormat
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.constant.ItemSortBy
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * An Android [DreamService] (screensaver) that shows TV series and movies from all libraries.
 * Use `adb shell am start -n "com.android.systemui/.Somnambulator"` to start after changing the
 * default screensaver in the device settings.
 */
class LibraryDreamService : DreamServiceCompat() {
	private val api: ApiClient by inject()
	private val userPreferences: UserPreferences by inject()

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()

		isInteractive = false
		isFullscreen = true

		setContent {
			var content by remember { mutableStateOf<DreamContent>(DreamContent.Logo) }

			LaunchedEffect(true) {
				delay(2.seconds)

				while (true) {
					content = getRandomLibraryShowcase() ?: DreamContent.Logo
					delay(30.seconds)
				}
			}

			DreamView(
				content = content,
				showClock = when (userPreferences[UserPreferences.clockBehavior]) {
					ClockBehavior.ALWAYS, ClockBehavior.IN_MENUS -> true
					else -> false
				}
			)
		}
	}

	private suspend fun getRandomLibraryShowcase(): DreamContent.LibraryShowcase? {
		try {
			val response by api.itemsApi.getItemsByUserId(
				includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
				recursive = true,
				sortBy = listOf(ItemSortBy.Random),
				limit = 5,
				imageTypes = listOf(ImageType.BACKDROP),
			)

			val item = response.items?.firstOrNull { item ->
				!item.backdropImageTags.isNullOrEmpty()
			} ?: return null

			val tag = item.backdropImageTags!!.randomOrNull() ?: item.imageTags?.get(ImageType.BACKDROP)

			val backdropUrl = api.imageApi.getItemImageUrl(
				itemId = item.id,
				imageType = ImageType.BACKDROP,
				tag = tag,
				format = ImageFormat.WEBP,
			)

			val backdrop = withContext(Dispatchers.IO) {
				Glide.with(baseContext).asBitmap().load(backdropUrl).submit().get()
			} ?: return null

			return DreamContent.LibraryShowcase(item, backdrop)
		} catch (err: ApiClientException) {
			Timber.e(err)
			return null
		}
	}
}
