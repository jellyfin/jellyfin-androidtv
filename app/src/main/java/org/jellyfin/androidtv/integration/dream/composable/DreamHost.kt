package org.jellyfin.androidtv.integration.dream.composable

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.ui.composable.rememberMediaItem
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageFormat
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.constant.ItemSortBy
import org.koin.compose.rememberKoinInject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@Composable
fun DreamHost() {
	val api = rememberKoinInject<ApiClient>()
	val userPreferences = rememberKoinInject<UserPreferences>()
	val mediaManager = rememberKoinInject<MediaManager>()
	val imageLoader = rememberKoinInject<ImageLoader>()
	val context = LocalContext.current

	var libraryShowcase by remember { mutableStateOf<DreamContent.LibraryShowcase?>(null) }
	val mediaItem by rememberMediaItem(mediaManager)

	LaunchedEffect(true) {
		delay(2.seconds)

		while (true) {
			libraryShowcase = getRandomLibraryShowcase(api, imageLoader, context)
			delay(30.seconds)
		}
	}

	DreamView(
		content = when {
			mediaItem != null -> DreamContent.NowPlaying(mediaItem)
			libraryShowcase != null -> libraryShowcase!!
			else -> DreamContent.Logo
		},
		showClock = when (userPreferences[UserPreferences.clockBehavior]) {
			ClockBehavior.ALWAYS, ClockBehavior.IN_MENUS -> true
			else -> false
		}
	)
}

private suspend fun getRandomLibraryShowcase(
	api: ApiClient,
	imageLoader: ImageLoader,
	context: Context
): DreamContent.LibraryShowcase? {
	try {
		val response by api.itemsApi.getItemsByUserId(
			includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			sortBy = listOf(ItemSortBy.Random),
			limit = 5,
			imageTypes = listOf(ImageType.BACKDROP),
			// TODO: Add preferences for these two settings
			maxOfficialRating = "PG-13",
			// hasParentalRating = true,
		)

		val item = response.items?.firstOrNull { item ->
			!item.backdropImageTags.isNullOrEmpty()
		} ?: return null

		Timber.i("Loading random library showcase item ${item.id}")

		val tag = item.backdropImageTags!!.randomOrNull()
			?: item.imageTags?.get(ImageType.BACKDROP)

		val backdropUrl = api.imageApi.getItemImageUrl(
			itemId = item.id,
			imageType = ImageType.BACKDROP,
			tag = tag,
			format = ImageFormat.WEBP,
		)

		val backdrop = withContext(Dispatchers.IO) {
			imageLoader.execute(
				request = ImageRequest.Builder(context).data(backdropUrl).build()
			).drawable?.toBitmap()
		} ?: return null

		return DreamContent.LibraryShowcase(item, backdrop)
	} catch (err: ApiClientException) {
		Timber.e(err)
		return null
	}
}
