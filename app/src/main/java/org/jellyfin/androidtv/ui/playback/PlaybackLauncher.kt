package org.jellyfin.androidtv.ui.playback

import android.content.Context
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import timber.log.Timber

/**
 * Utility class to launch the playback UI for an item.
 */
class PlaybackLauncher(
	private val mediaManager: MediaManager,
	private val videoQueueManager: VideoQueueManager,
	private val navigationRepository: NavigationRepository,
	private val userPreferences: UserPreferences,
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
		baseItem: BaseItemDto? = null,
	) {

		var finalItems = items
		var finalItemsPosition = itemsPosition
		if (baseItem != null) {
			Timber.i("Base item name = ${baseItem.name}, mediaType = ${baseItem.mediaType}")
			if (baseItem.mediaType == MediaType.VIDEO) {
				// The parent collection asserts it's only for videos, so
				// remove any non-videos
				val initialSize = items.size
				finalItems = items.filter { it.mediaType == MediaType.VIDEO }
				if (initialSize != finalItems.size) {
					Timber.e("Collection of videos has non-video items!")
					finalItems.forEach { Timber.w("Collection item, name = ${it.name}, path = '${it.path}', mediaType = ${it.mediaType}, id = ${it.id}, serverId = ${it.serverId}, playlistItemId = ${it.playlistItemId}, container = ${it.container}, ") }

					// Now we need to adjust the itemsPosition by the number
					// of non-videos items removed below it.
					if (finalItemsPosition > 0) {
						finalItemsPosition -= items.foldIndexed(0, {
								index, acc, it ->
							if (it.mediaType == MediaType.VIDEO || index >= finalItemsPosition) acc else acc + 1
						})
						if (finalItemsPosition < 0) {
							finalItemsPosition = 0
						}
					}
				}
			}
		}

		val isAudio = finalItems.any { it.mediaType == MediaType.AUDIO }
		Timber.i("launch ${finalItems.size} items; isAudio = $isAudio")

		if (isAudio) {
			mediaManager.playNow(context, finalItems, finalItemsPosition, shuffle)
			navigationRepository.navigate(Destinations.nowPlaying)
		} else {
			val finalItems = if (shuffle) finalItems.shuffled() else finalItems

			videoQueueManager.setCurrentVideoQueue(finalItems.toList())
			videoQueueManager.setCurrentMediaPosition(finalItemsPosition)

			if (finalItems.isEmpty()) return

			if (userPreferences[UserPreferences.useExternalPlayer] && finalItems.all { it.supportsExternalPlayer }) {
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
}
