package org.jellyfin.androidtv.integration

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.tvprovider.media.tv.*
import androidx.tvprovider.media.tv.TvContractCompat.WatchNextPrograms
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.androidtv.util.apiclient.getNextUpEpisodes
import org.jellyfin.androidtv.util.apiclient.getUserViews
import org.jellyfin.androidtv.util.dp
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.drawing.ImageFormat
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.ImageOptions
import org.jellyfin.apiclient.model.entities.SeriesStatus
import org.jellyfin.apiclient.model.querying.ItemFields
import org.jellyfin.apiclient.model.querying.NextUpQuery

import org.koin.java.KoinJavaComponent.get

/**
 * Manages channels on the android tv home screen
 *
 * More info: https://developer.android.com/training/tv/discovery/recommendations-channel
 */
class LeanbackChannelWorker(
	private val context: Context,
	val workerParams: WorkerParameters,
	private val apiClient: ApiClient
) : CoroutineWorker(context, workerParams) {
	companion object {
		/**
		 * Amount of ticks found in a millisecond, used for calculation
		 */
		private const val TICKS_IN_MILLISECOND = 10000

		const val SINGLE_UPDATE_REQUEST_NAME = "LeanbackChannelSingleUpdateRequest"
		const val PERIODIC_UPDATE_REQUEST_NAME = "LeanbackChannelPeriodicUpdateRequest"
	}

	/**
	 * Check if the app can use Leanback features and is API level 26 or higher
	 */
	private val isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
		&& context.packageManager.hasSystemFeature("android.software.leanback")

	/**
	 * Update all channels for the currently authenticated user
	 */
	override suspend fun doWork(): Result = when {
		// Fail when not supported
		!isSupported -> Result.failure()
		// Retry later if no authenticated user is found
		TvApp.getApplication().currentUser == null -> Result.retry()
		else -> {
			// Update various channels
			updateMyMedia()
			updateWatchNext()

			// Success!
			Result.success()
		}
	}

	/**
	 * Get the uri for a channel or create it if it doesn't exist.
	 * Uses the [settings] parameter to update or create the channel.
	 * The [name] parameter is used to store the id and should be unique.
	 */
	private fun getChannelUri(name: String, settings: Channel): Uri {
		val store = context.getSharedPreferences("leanback_channels", Context.MODE_PRIVATE)

		val uri = if (store.contains(name)) {
			// Retrieve uri and update content resolver
			Uri.parse(store.getString(name, null)).also { uri ->
				context.contentResolver.update(uri, settings.toContentValues(), null, null)
			}
		} else {
			// Create new channel and save uri
			context.contentResolver.insert(TvContractCompat.Channels.CONTENT_URI, settings.toContentValues())!!.also { uri ->
				store.edit().putString(name, uri.toString()).apply()
			}

			// Set as default row to display (we can request one row to automatically be added to the home screen)
			// Should be enabled when we add a row that we want to display by default
			// TvContractCompat.requestChannelBrowsable(application, ContentUris.parseId(uri))
		}

		// Update logo
		ResourcesCompat.getDrawable(context.resources, R.drawable.ic_jellyfin, null)?.let {
			ChannelLogoUtils.storeChannelLogo(context, ContentUris.parseId(uri), it.toBitmap(80.dp, 80.dp))
		}

		return uri
	}

	/**
	 * Updates the "my media" row with current media libraries
	 */
	private suspend fun updateMyMedia() {
		// Get channel
		val channelUri = getChannelUri("my_media", Channel.Builder()
			.setType(TvContractCompat.Channels.TYPE_PREVIEW)
			.setDisplayName(context.getString(R.string.lbl_my_media))
			.setAppLinkIntent(Intent(context, StartupActivity::class.java))
			.build())

		val response = apiClient.getUserViews() ?: return

		// Delete current items
		context.contentResolver.delete(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null)

		// Add new items
		context.contentResolver.bulkInsert(TvContractCompat.PreviewPrograms.CONTENT_URI, response.items.map { item ->
			val imageUri = if (item.hasPrimaryImage) Uri.parse(apiClient.GetImageUrl(item, ImageOptions()))
			else Uri.parse(ImageUtils.getResourceUrl(context, R.drawable.tile_land_tv))

			PreviewProgram.Builder()
				.setChannelId(ContentUris.parseId(channelUri))
				.setType(TvContractCompat.PreviewPrograms.TYPE_CHANNEL)
				.setTitle(item.name)
				.setPosterArtUri(imageUri)
				.setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9)
				.setIntent(Intent(context, StartupActivity::class.java).apply {
					putExtra(StartupActivity.ITEM_ID, item.id)
					putExtra(StartupActivity.ITEM_IS_USER_VIEW, true)
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
		context.contentResolver.delete(WatchNextPrograms.CONTENT_URI, null, null)

		// Get user or return if no user is found (not authenticated)
		val user = TvApp.getApplication().currentUser ?: return@withContext

		// Get new items
		val response = apiClient.getNextUpEpisodes(NextUpQuery().apply {
			userId = user.id
			imageTypeLimit = 1
			limit = 10
			fields = arrayOf(ItemFields.DateCreated)
		})

		// Add new items
		response?.items?.let { items ->
			context.contentResolver.bulkInsert(
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
	private suspend fun getBaseItemAsWatchNextProgram(item: BaseItemDto) = WatchNextProgram.Builder().apply {
		val preferParentThumb = get(UserPreferences::class.java)[UserPreferences.seriesThumbnailsEnabled]
		val seriesItem = apiClient.getItem(item.seriesId)

		setInternalProviderId(item.id)
		setType(WatchNextPrograms.TYPE_TV_EPISODE)
		setTitle("${item.seriesName} - ${item.name}")

		// Poster image
		setPosterArtAspectRatio(WatchNextPrograms.ASPECT_RATIO_16_9)
		if (preferParentThumb && seriesItem != null && seriesItem.hasThumb) {
			setPosterArtUri(Uri.parse(apiClient.GetImageUrl(seriesItem, ImageOptions().apply {
				format = ImageFormat.Png
				height = 288
				width = 512
				imageType = org.jellyfin.apiclient.model.entities.ImageType.Thumb
			})))
		}
		else {
			setPosterArtUri(Uri.parse(apiClient.GetImageUrl(item, ImageOptions().apply {
				format = ImageFormat.Png
				height = 288
				width = 512
			})))
		}

		// Use date created or fallback to current time if unavailable
		setLastEngagementTimeUtcMillis(item.dateCreated?.time ?: System.currentTimeMillis())

		when {
			// User has started playing the episode
			item.canResume -> {
				setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
				setLastPlaybackPositionMillis((item.resumePositionTicks / TICKS_IN_MILLISECOND).toInt())
			}
			// Most recently aired episode
			seriesItem?.userData?.unplayedItemCount == 1 && seriesItem.seriesStatus == SeriesStatus.Continuing -> {
				setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_NEW)
			}
			// Default
			else -> {
				setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_NEXT)
			}
		}

		// Episode runtime has been determined
		if (item.runTimeTicks != null) {
			setDurationMillis((item.runTimeTicks / TICKS_IN_MILLISECOND).toInt())
		}

		// Set intent to open the episode
		setIntent(Intent(context, StartupActivity::class.java).apply {
			putExtra(StartupActivity.ITEM_ID, item.id)
		})
	}.build()
}
