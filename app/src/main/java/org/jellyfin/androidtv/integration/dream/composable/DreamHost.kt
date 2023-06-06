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
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.koin.compose.koinInject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@Composable
fun DreamHost() {
	val api = koinInject<ApiClient>()
	val userPreferences = koinInject<UserPreferences>()
	val mediaManager = koinInject<MediaManager>()
	val imageLoader = koinInject<ImageLoader>()
	val context = LocalContext.current

	var libraryShowcase by remember { mutableStateOf<DreamContent.LibraryShowcase?>(null) }
	val mediaItem by rememberMediaItem(mediaManager)

	LaunchedEffect(true) {
		delay(2.seconds)

		while (true) {
			val requireParentalRating = userPreferences[UserPreferences.screensaverAgeRatingRequired]
			val maxOfficialRating = userPreferences[UserPreferences.screensaverAgeRatingMax]
			libraryShowcase = getRandomLibraryShowcase(context, api, maxOfficialRating, requireParentalRating, imageLoader)
			delay(30.seconds)
		}
	}

	DreamView(
		content = when {
			mediaItem?.mediaType == MediaType.AUDIO -> DreamContent.NowPlaying(mediaItem)
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
	context: Context,
	api: ApiClient,
	maxParentalRating: Int,
	requireParentalRating: Boolean,
	imageLoader: ImageLoader,
): DreamContent.LibraryShowcase? {
	try {
		val response by api.itemsApi.getItems(
			includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			sortBy = listOf(ItemSortBy.RANDOM),
			limit = 5,
			imageTypes = listOf(ImageType.BACKDROP),
			maxOfficialRating = if (maxParentalRating == -1) null else maxParentalRating.toString(),
			hasParentalRating = if (requireParentalRating) true else null,
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
