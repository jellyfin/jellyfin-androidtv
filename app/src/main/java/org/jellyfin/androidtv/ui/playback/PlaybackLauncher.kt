package org.jellyfin.androidtv.ui.playback

import android.content.Context
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Utility class to launch the playback UI for an item.
 */
class PlaybackLauncher(
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
	fun launch(context: Context, items: Collection<BaseItemDto>, position: Int? = null, replace: Boolean = false, itemsPosition: Int = 0) {
		videoQueueManager.setCurrentVideoQueue(items.toList())
		videoQueueManager.setCurrentMediaPosition(itemsPosition)

		if (items.isEmpty()) return

		if (userPreferences[UserPreferences.useExternalPlayer] && items.all { it.supportsExternalPlayer }) {
			context.startActivity(ActivityDestinations.externalPlayer(context, position?.milliseconds ?: Duration.ZERO))
		} else if (userPreferences[UserPreferences.playbackRewriteVideoEnabled]) {
			val destination = Destinations.playbackRewritePlayer(position)
			navigationRepository.navigate(destination, replace)
		} else {
			val destination = Destinations.videoPlayer(position)
			navigationRepository.navigate(destination, replace)
		}
	}
}
