package org.jellyfin.androidtv.ui.playback

import android.content.Context
import android.widget.Toast
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import java.util.Random
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
	private val shuffleRandom: Random = Random(),
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

	/**
	 * Ordinary library video played by the legacy internal player needs source metadata.
	 * Audio, live TV, and the external/rewrite players keep their previous behavior.
	 */
	private val BaseItemDto.isUnprobedLibraryVideo
		get() = when (type) {
			BaseItemKind.MOVIE,
			BaseItemKind.EPISODE,
			BaseItemKind.VIDEO,
				-> mediaSources.isNullOrEmpty()

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
		if (items.any { it.mediaType == MediaType.AUDIO }) {
			mediaManager.playNow(context, items, itemsPosition, shuffle)
			navigationRepository.navigate(Destinations.nowPlaying)
			return
		}

		val queue = if (shuffle) items.shuffled(shuffleRandom) else items
		// Index applies to the shuffled queue. Reject before replacing the active session.
		if (itemsPosition !in queue.indices) {
			showCannotPlay(context)
			return
		}

		val startingItem = queue[itemsPosition]
		val useExternalPlayer = userPreferences[UserPreferences.useExternalPlayer] &&
			queue.all { it.supportsExternalPlayer }
		val useRewritePlayer = !useExternalPlayer &&
			userPreferences[UserPreferences.playbackRewriteVideoEnabled]
		if (!useExternalPlayer && !useRewritePlayer && startingItem.isUnprobedLibraryVideo) {
			showCannotPlay(context)
			return
		}

		videoQueueManager.setCurrentVideoQueue(queue.toList())
		videoQueueManager.setCurrentMediaPosition(itemsPosition)

		if (useExternalPlayer) {
			val startPosition = position?.milliseconds ?: Duration.ZERO
			context.startActivity(ActivityDestinations.externalPlayer(context, startPosition))
		} else if (useRewritePlayer) {
			navigationRepository.navigate(Destinations.videoPlayerNew(position), replace)
		} else {
			navigationRepository.navigate(Destinations.videoPlayer(position), replace)
		}
	}

	private fun showCannotPlay(context: Context) {
		Toast.makeText(context, R.string.msg_cannot_play, Toast.LENGTH_LONG).show()
	}
}
