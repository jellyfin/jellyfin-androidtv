package org.jellyfin.androidtv.ui.playback

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.serializer.toUUID
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Utility class to launch the playback UI for an item.
 */
class PlaybackLauncher(
	private val mediaManager: MediaManager,
	private val videoQueueManager: VideoQueueManager,
	private val navigationRepository: NavigationRepository,
	private val userPreferences: UserPreferences,
	private val api: ApiClient,
) {
	private val BaseItemDto.supportsExternalPlayer
		get() = when (type) {
			BaseItemKind.MOVIE,
			BaseItemKind.EPISODE,
			BaseItemKind.VIDEO,
			BaseItemKind.SERIES,
			BaseItemKind.SEASON,
			BaseItemKind.RECORDING,
			BaseItemKind.TV_CHANNEL,
			BaseItemKind.PROGRAM,
				-> true

			else -> false
		}

	@JvmOverloads
	fun launch(
		context: Context,
		items: List<BaseItemDto>,
		position: Int? = null,
		replace: Boolean = false,
		itemsPosition: Int = 0,
		shuffle: Boolean = false,
	) {
		val isAudio = items.any { it.mediaType == MediaType.AUDIO }

		if (isAudio) {
			mediaManager.playNow(context, items, itemsPosition, shuffle)
			navigationRepository.navigate(Destinations.nowPlaying)
		} else {
			val items = if (shuffle) items.shuffled() else items

			videoQueueManager.setCurrentVideoQueue(items.toList())
			videoQueueManager.setCurrentMediaPosition(itemsPosition)

			if (items.isEmpty()) return

			if (userPreferences[UserPreferences.useExternalPlayer] && items.all { it.supportsExternalPlayer }) {
				context.startActivity(ActivityDestinations.externalPlayer(context, position?.milliseconds ?: Duration.ZERO))
			} else if (userPreferences[UserPreferences.playbackRewriteVideoEnabled]) {
				val destination = Destinations.videoPlayerNew(position)
				navigationRepository.navigate(destination, replace)
			} else {
				val destination = Destinations.videoPlayer(position)
				navigationRepository.navigate(destination, replace)
			}
		}
	}

	/**
	 * Launch playback from Watch Next deep link.
	 * Fetches the item from the server and starts playback at the specified position.
	 * 
	 * Note: serverId is received but not validated against the current API client.
	 * This assumes the app is already connected to the correct server when the deep link is invoked.
	 * Multi-server setups should handle server switching at a higher level before calling this method.
	 */
	fun playFromWatchNext(
		lifecycleOwner: LifecycleOwner,
		context: Context,
		serverId: String,
		itemId: String,
		positionMs: Long
	) {
		lifecycleOwner.lifecycleScope.launch {
			try {
				val itemUuid = itemId.toUUID()
				val item = withContext(Dispatchers.IO) {
					api.userLibraryApi.getItem(itemUuid).content
				}

				// Launch playback with the specified position
				launch(context, listOf(item), position = positionMs.toInt())
			} catch (e: ApiClientException) {
				Timber.e(e, "Failed to fetch item for Watch Next playback: itemId=$itemId")
			} catch (e: Exception) {
				Timber.e(e, "Failed to start Watch Next playback: itemId=$itemId")
			}
		}
	}
}
