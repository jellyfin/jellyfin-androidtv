package org.jellyfin.androidtv.channels

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.tvprovider.media.tv.TvContractCompat.WatchNextPrograms
import androidx.tvprovider.media.tv.WatchNextProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.startup.StartupActivity
import org.jellyfin.androidtv.util.apiclient.getNextUpEpisodes
import org.jellyfin.apiclient.model.drawing.ImageFormat
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.ImageOptions
import org.jellyfin.apiclient.model.querying.ItemFields
import org.jellyfin.apiclient.model.querying.NextUpQuery

/**
 * Manages channels on the android tv home screen
 *
 * More info: https://developer.android.com/training/tv/discovery/recommendations-channel
 */
class ChannelManager {
	private companion object {
		/**
		 * Amount of ticks found in a millisecond, used for calculation
		 */
		private const val TICKS_IN_MILLISECOND = 10000
	}

	private val application = TvApp.getApplication()

	/**
	 * Check if the app can use Leanback features and is API level 26 or higher
	 */
	private val isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
		&& application.packageManager.hasSystemFeature("android.software.leanback")

	/**
	 * Update all channels for the currently authenticated user
	 */
	fun update() {
		// Don't do anything if not supported
		if (!isSupported) return

		// Launch in separate coroutine scope
		GlobalScope.launch {
			updateWatchNext()
		}
	}

	/**
	 * Updates the "watch next" row with new and unfinished episodes
	 * does not include movies, music or other types of media
	 */
	private suspend fun updateWatchNext() = withContext(Dispatchers.Default) {
		// Delete current items
		application.contentResolver.delete(WatchNextPrograms.CONTENT_URI, null, null)

		// Get user or return if no user is found (not authenticated)
		val user = application.currentUser ?: return@withContext

		// Get new items
		val response = application.apiClient.getNextUpEpisodes(NextUpQuery().apply {
			userId = user.id
			imageTypeLimit = 1
			limit = 10
			fields = arrayOf(ItemFields.DateCreated)
		})

		// Add new items
		response?.items?.forEach { item ->
			application.contentResolver.insert(
				WatchNextPrograms.CONTENT_URI,
				getBaseItemAsWatchNextProgram(item).toContentValues()
			)
		}
	}

	/**
	 * Convert [BaseItemDto] to [WatchNextProgram]
	 *
	 * Assumes the item type is "episode"
	 */
	private fun getBaseItemAsWatchNextProgram(item: BaseItemDto) = WatchNextProgram.Builder().apply {
		setInternalProviderId(item.id)
		setType(WatchNextPrograms.TYPE_TV_EPISODE)
		setTitle("${item.seriesName} - ${item.name}")

		// Poster image
		setPosterArtAspectRatio(WatchNextPrograms.ASPECT_RATIO_16_9)
		setPosterArtUri(Uri.parse(application.apiClient.GetImageUrl(item, ImageOptions().apply {
			format = ImageFormat.Png
			height = 288
			width = 512
		})))

		// Use date created or fallback to current time if unavailable
		setLastEngagementTimeUtcMillis(item.dateCreated?.time ?: System.currentTimeMillis())

		when {
			// User has started playing the episode
			item.canResume -> {
				setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
				setLastPlaybackPositionMillis((item.resumePositionTicks / TICKS_IN_MILLISECOND).toInt())
			}
			// Episode runtime has been determined
			item.runTimeTicks != null -> {
				setDurationMillis((item.runTimeTicks / TICKS_IN_MILLISECOND).toInt())
			}
			// First episode of the season
			item.indexNumber == 0 -> {
				setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_NEW)
			}
			// Default
			else -> {
				setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_NEXT)
			}
		}

		// Set intent to open the episode
		setIntent(Intent(application, StartupActivity::class.java).apply {
			putExtra("ItemId", item.id)
		})
	}.build()
}
