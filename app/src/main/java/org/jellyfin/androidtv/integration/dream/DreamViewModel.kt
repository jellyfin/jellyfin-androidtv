package org.jellyfin.androidtv.integration.dream

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SuppressLint("StaticFieldLeak")
class DreamViewModel(
	private val api: ApiClient,
	private val imageLoader: ImageLoader,
	private val context: Context,
	playbackManager: PlaybackManager,
	private val userPreferences: UserPreferences,
) : ViewModel() {
	@OptIn(ExperimentalCoroutinesApi::class)
	private val _mediaContent = playbackManager.queue.entry
		.map { entry ->
			entry
				?.takeIf { it.visibleInScreensaver }
				?.baseItem
				?.let { baseItem -> DreamContent.NowPlaying(entry, baseItem) }
		}
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

	private val _libraryContent = flow {
		// Load first library item after 2 seconds
		// to force the logo at the start of the screensaver
		emit(null)
		delay(2.seconds)

		val requireParentalRating = userPreferences[UserPreferences.screensaverAgeRatingRequired]
		val maxParentalRating = userPreferences[UserPreferences.screensaverAgeRatingMax]
		emitAll(
			getRandomLibraryShowcaseItems(
				requireParentalRating = requireParentalRating,
				maxParentalRating = maxParentalRating,
				// A batch size of 60 should be equal to 30 minutes of items
				batchSize = 60,
				emitDelay = 30.seconds,
				noItemsDelay = 2.minutes,
				errorDelay = 3.seconds,
			)
		)
	}
		.distinctUntilChanged()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

	val content = combine(_mediaContent, _libraryContent) { mediaContent, libraryContent ->
		mediaContent ?: libraryContent ?: DreamContent.Logo
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(),
		initialValue = _mediaContent.value ?: _libraryContent.value ?: DreamContent.Logo,
	)

	private fun getRandomLibraryShowcaseItems(
		requireParentalRating: Boolean,
		maxParentalRating: Int,
		batchSize: Int,
		emitDelay: Duration,
		noItemsDelay: Duration,
		errorDelay: Duration,
	): Flow<DreamContent.LibraryShowcase?> = flow {
		while (true) {
			val items = try {
				withContext(Dispatchers.IO) {
					val response by api.itemsApi.getItems(
						includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
						recursive = true,
						sortBy = listOf(ItemSortBy.RANDOM),
						limit = batchSize,
						imageTypes = listOf(ImageType.BACKDROP),
						maxOfficialRating = if (maxParentalRating == -1) null else maxParentalRating.toString(),
						hasParentalRating = if (requireParentalRating) true else null,
					)
					response.items
				}
			} catch (err: ApiClientException) {
				Timber.e(err)
				null
			}

			if (items == null) {
				emit(null)
				delay(errorDelay)
			} else if (items.isEmpty()) {
				emit(null)
				delay(noItemsDelay)
			} else {
				for (item in items) {
					if (item.itemBackdropImages.isEmpty()) continue
					val showcase = item.asLibraryShowcase() ?: continue
					emit(showcase)
					delay(emitDelay)
				}
			}
		}
	}.cancellable()

	private suspend fun BaseItemDto.asLibraryShowcase(): DreamContent.LibraryShowcase? = withContext(Dispatchers.IO) {
		val logoUrl = itemImages[ImageType.LOGO]?.getUrl(api)
		val backdropUrl = itemBackdropImages.randomOrNull()?.getUrl(api)

		// Require a backdrop
		if (backdropUrl == null) return@withContext null

		// Only attempt to load logo if there is one, wrap in async {} to load it parallel with the backdrop
		val logo = logoUrl?.let { url ->
			async {
				imageLoader.execute(
					request = ImageRequest.Builder(context).data(url).build()
				).image?.toBitmap()
			}
		}

		val backdrop = imageLoader.execute(
			request = ImageRequest.Builder(context).data(backdropUrl).build()
		).image?.toBitmap()

		if (backdrop == null) null
		else DreamContent.LibraryShowcase(this@asLibraryShowcase, backdrop, logo?.await())
	}
}
