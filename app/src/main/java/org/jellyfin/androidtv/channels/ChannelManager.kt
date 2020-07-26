package org.jellyfin.androidtv.channels

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import androidx.tvprovider.media.tv.*
import androidx.tvprovider.media.tv.TvContractCompat.WatchNextPrograms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.startup.StartupActivity
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.apiclient.getNextUpEpisodes
import org.jellyfin.androidtv.util.apiclient.getUserViews
import org.jellyfin.androidtv.util.dp
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
			updateMyMedia()
			updateWatchNext()
		}
	}

	/**
	 * Get the uri for a channel or create it if it doesn't exist.
	 * Uses the [settings] parameter to update or create the channel.
	 * The [name] parameter is used to store the id and should be unique.
	 */
	private fun getChannelUri(name: String, settings: Channel): Uri {
		val store = application.getSharedPreferences("leanback_channels", Context.MODE_PRIVATE)

		val uri = if (store.contains(name)) {
			// Retrieve uri and update content resolver
			Uri.parse(store.getString(name, null)).also { uri ->
				application.contentResolver.update(uri, settings.toContentValues(), null, null)
			}
		} else {
			// Create new channel and save uri
			application.contentResolver.insert(TvContractCompat.Channels.CONTENT_URI, settings.toContentValues())!!.also { uri ->
				store.edit().putString(name, uri.toString()).apply()
			}

			// Set as default row to display (we can request one row to automatically be added to the home screen)
			// Should be enabled when we add a row that we want to display by default
			// TvContractCompat.requestChannelBrowsable(application, ContentUris.parseId(uri))
		}

		// Update logo
		ChannelLogoUtils.storeChannelLogo(application, ContentUris.parseId(uri), application.resources.getDrawable(R.drawable.ic_jellyfin, null).toBitmap(80.dp, 80.dp))

		return uri
	}

	/**
	 * Updates the "my media" row with current media libraries
	 */
	private suspend fun updateMyMedia() {
		// Get channel
		val channelUri = getChannelUri("my_media", Channel.Builder()
			.setType(TvContractCompat.Channels.TYPE_PREVIEW)
			.setDisplayName(application.getString(R.string.lbl_my_media))
			.setAppLinkIntent(Intent(application, StartupActivity::class.java))
			.build())

		val response = application.apiClient.getUserViews() ?: return

		// Delete current items
		application.contentResolver.delete(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null)

		// Add new items
		application.contentResolver.bulkInsert(TvContractCompat.PreviewPrograms.CONTENT_URI, response.items.map { item ->
			val imageUri = if (item.hasPrimaryImage) Uri.parse(application.apiClient.GetImageUrl(item, ImageOptions()))
			else Uri.parse(ImageUtils.getResourceUrl(R.drawable.tile_land_tv))

			PreviewProgram.Builder()
				.setChannelId(ContentUris.parseId(channelUri))
				.setType(TvContractCompat.PreviewPrograms.TYPE_CHANNEL)
				.setTitle(item.name)
				.setPosterArtUri(imageUri)
				.setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9)
				.setIntent(Intent(application, StartupActivity::class.java).apply {
					putExtra("ItemId", item.id)
					putExtra("ItemIsUserView", true)
				})
				.build().toContentValues()
		}.toTypedArray())
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
		response?.items?.let { items ->
			application.contentResolver.bulkInsert(
				WatchNextPrograms.CONTENT_URI,
				items.map { item -> getBaseItemAsWatchNextProgram(item).toContentValues() }.toTypedArray()
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
