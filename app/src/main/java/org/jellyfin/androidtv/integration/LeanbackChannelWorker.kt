package org.jellyfin.androidtv.integration

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.TvContractCompat.WatchNextPrograms
import androidx.tvprovider.media.tv.WatchNextProgram
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.integration.provider.ImageProvider
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.androidtv.util.dp
import org.jellyfin.androidtv.util.sdk.isUsable
import org.jellyfin.androidtv.util.stripHtml
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.TimeoutException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.extensions.ticks
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

/**
 * Manages channels on the android tv home screen.
 *
 * More info: https://developer.android.com/training/tv/discovery/recommendations-channel.
 */
class LeanbackChannelWorker(
	private val context: Context,
	workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams), KoinComponent {
	companion object {
		const val PERIODIC_UPDATE_REQUEST_NAME = "LeanbackChannelPeriodicUpdateRequest"
	}

	private val api by inject<ApiClient>()
	private val userPreferences by inject<UserPreferences>()
	private val userViewsRepository by inject<UserViewsRepository>()
	private val imageHelper by inject<ImageHelper>()

	/**
	 * Check if the app can use Leanback features and is API level 26 or higher.
	 */
	private val isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
		// Check for leanback support
		context.packageManager.hasSystemFeature("android.software.leanback")
		// Check for "android.media.tv" provider to workaround a false-positive in the previous check
		&& context.packageManager.resolveContentProvider(TvContractCompat.AUTHORITY, 0) != null

	/**
	 * Update all channels for the currently authenticated user.
	 */
	override suspend fun doWork(): Result = when {
		// Fail when not supported
		!isSupported -> Result.failure()
		// Retry later if no authenticated user is found
		!api.isUsable -> Result.retry()
		else -> try {
			// Get next up episodes
			val (resumeItems, nextUpItems) = getNextUpItems()
			// Get latest media
			val (latestEpisodes, latestMovies, latestMedia) = getLatestMedia()
			val myMedia = getMyMedia()
			// Delete current items from the channels
			context.contentResolver.delete(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null)

			// Get channel URIs
			val latestMediaChannel = getChannelUri(
				"latest_media", Channel.Builder()
					.setType(TvContractCompat.Channels.TYPE_PREVIEW)
					.setDisplayName(context.getString(R.string.home_section_latest_media))
					.setAppLinkIntent(Intent(context, StartupActivity::class.java))
					.build(),
				default = true
			)
			val myMediaChannel = getChannelUri(
				"my_media", Channel.Builder()
					.setType(TvContractCompat.Channels.TYPE_PREVIEW)
					.setDisplayName(context.getString(R.string.lbl_my_media))
					.setAppLinkIntent(Intent(context, StartupActivity::class.java))
					.build()
			)
			val nextUpChannel = getChannelUri(
				"next_up", Channel.Builder()
					.setType(TvContractCompat.Channels.TYPE_PREVIEW)
					.setDisplayName(context.getString(R.string.lbl_next_up))
					.setAppLinkIntent(Intent(context, StartupActivity::class.java))
					.build()
			)
			val latestMoviesChannel = getChannelUri(
				"latest_movies", Channel.Builder()
					.setType(TvContractCompat.Channels.TYPE_PREVIEW)
					.setDisplayName(context.getString(R.string.lbl_movies))
					.setAppLinkIntent(Intent(context, StartupActivity::class.java))
					.build()
			)
			val latestEpisodesChannel = getChannelUri(
				"latest_episodes", Channel.Builder()
					.setType(TvContractCompat.Channels.TYPE_PREVIEW)
					.setDisplayName(context.getString(R.string.lbl_new_episodes))
					.setAppLinkIntent(Intent(context, StartupActivity::class.java))
					.build()
			)
			val preferParentThumb = userPreferences[UserPreferences.seriesThumbnailsEnabled]

			// Add new items
			arrayOf(
				nextUpItems to nextUpChannel,
				latestMedia to latestMediaChannel,
				latestMovies to latestMoviesChannel,
				latestEpisodes to latestEpisodesChannel,
				myMedia to myMediaChannel,
			).forEach { (items, channel) ->
				if (channel == null) {
					Timber.e("Skipping channel because it was not available")
				} else {
					items.map { item ->
						createPreviewProgram(
							channel,
							item,
							preferParentThumb
						)
					}.let {
						context.contentResolver.bulkInsert(
							TvContractCompat.PreviewPrograms.CONTENT_URI,
							it.toTypedArray()
						)
					}
				}
			}
			updateWatchNext(resumeItems + nextUpItems)

			// Success!
			Result.success()
		} catch (err: TimeoutException) {
			Timber.w(err, "Server unreachable, trying again later")

			Result.retry()
		} catch (err: ApiClientException) {
			Timber.e(err, "SDK error, trying again later")

			Result.retry()
		}
	}

	/**
	 * Get the uri for a channel or create it if it doesn't exist. Uses the [settings] parameter to
	 * update or create the channel. The [name] parameter is used to store the id and should be
	 * unique.
	 */
	private fun getChannelUri(name: String, settings: Channel, default: Boolean = false): Uri? {
		val store = context.getSharedPreferences("leanback_channels", Context.MODE_PRIVATE)
		var uri: Uri? = null

		// Try and re-use our existing channel definition
		if (store.contains(name)) {
			uri = store.getString(name, null)?.toUri()

			if (uri != null) {
				val result = context.contentResolver.update(uri, settings.toContentValues(), null, null)
				// If we did not affect exactly 1 row there might be something wrong, so recreate it
				if (result != 1) uri = null
			}
		}

		if (uri == null) {
			// Create new channel
			uri = context.contentResolver.insert(
				TvContractCompat.Channels.CONTENT_URI,
				settings.toContentValues()
			)

			// Set as default row to display (we can request one row to automatically be added to the home screen)
			if (uri != null && default) {
				TvContractCompat.requestChannelBrowsable(context, ContentUris.parseId(uri))
			}

			// Save uri to shared preferences
			store.edit { putString(name, uri?.toString()) }
		}

		// Update logo
		if (uri != null) {
			ResourcesCompat.getDrawable(context.resources, R.mipmap.app_icon, context.theme)?.let {
				ChannelLogoUtils.storeChannelLogo(
					context,
					ContentUris.parseId(uri),
					it.toBitmap(80.dp(context), 80.dp(context))
				)
			}
		}

		return uri
	}

	/**
	 * Updates the "my media" row with current media libraries.
	 */
	@Suppress("RestrictedApi")
	private suspend fun getMyMedia(): List<BaseItemDto> {
		val response by api.userViewsApi.getUserViews(includeHidden = false)

		// Add new items
		return response.items
			.filter { userViewsRepository.isSupported(it.collectionType) }
	}

	/**
	 * Gets the poster art for an item. Uses the [preferParentThumb] parameter to fetch the series
	 * image when preferred.
	 */
	private fun BaseItemDto.getPosterArtImageUrl(
		preferParentThumb: Boolean
	): Uri = when {
		type == BaseItemKind.MOVIE || type == BaseItemKind.SERIES -> itemImages[ImageType.PRIMARY]
		(preferParentThumb || !itemImages.contains(ImageType.PRIMARY)) && parentImages.contains(ImageType.THUMB) -> parentImages[ImageType.THUMB]
		else -> itemImages[ImageType.PRIMARY]
	}.let { image ->
		ImageProvider.getImageUri(image?.getUrl(api) ?: imageHelper.getResourceUrl(context, R.drawable.tile_land_tv))
	}

	/**
	 * Gets the resume and next up episodes. The returned pair contains two lists:
	 * 1. resume items
	 * 2. next up items
	 */
	private suspend fun getNextUpItems(): Pair<List<BaseItemDto>, List<BaseItemDto>> =
		withContext(Dispatchers.IO) {
			val resume = async {
				api.itemsApi.getResumeItems(
					fields = ItemRepository.itemFields,
					imageTypeLimit = 1,
					limit = 10,
					mediaTypes = listOf(MediaType.VIDEO),
					includeItemTypes = listOf(BaseItemKind.EPISODE, BaseItemKind.MOVIE),
					excludeActiveSessions = true,
				).content.items
			}

			val nextUp = async {
				api.tvShowsApi.getNextUp(
					imageTypeLimit = 1,
					limit = 10,
					enableResumable = false,
					fields = ItemRepository.itemFields,
				).content.items
			}

			// Concat
			Pair(resume.await(), nextUp.await())
		}

	private suspend fun getLatestMedia(): Triple<List<BaseItemDto>, List<BaseItemDto>, List<BaseItemDto>> =
		withContext(Dispatchers.IO) {
			val latestEpisodes = async {
				api.userLibraryApi.getLatestMedia(
					fields = ItemRepository.itemFields,
					limit = 50,
					includeItemTypes = listOf(BaseItemKind.EPISODE),
					isPlayed = false
				).content
			}

			val latestMovies = async {
				api.userLibraryApi.getLatestMedia(
					fields = ItemRepository.itemFields,
					limit = 50,
					includeItemTypes = listOf(BaseItemKind.MOVIE),
					isPlayed = false
				).content
			}

			val latestMedia = async {
				api.userLibraryApi.getLatestMedia(
					fields = ItemRepository.itemFields,
					limit = 50,
					includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
					isPlayed = false
				).content
			}

			// Concat
			Triple(latestEpisodes.await(), latestMovies.await(), latestMedia.await())
		}

	@SuppressLint("RestrictedApi")
	private fun createPreviewProgram(
		channelUri: Uri,
		item: BaseItemDto,
		preferParentThumb: Boolean
	): ContentValues {
		val imageUri = item.getPosterArtImageUrl(preferParentThumb)
		val seasonString = item.parentIndexNumber?.toString().orEmpty()

		val episodeString = when {
			item.indexNumberEnd != null && item.indexNumber != null ->
				"${item.indexNumber}-${item.indexNumberEnd}"

			else -> item.indexNumber?.toString().orEmpty()
		}

		return PreviewProgram.Builder()
			.setChannelId(ContentUris.parseId(channelUri))
			.setType(
				when (item.type) {
					BaseItemKind.SERIES -> WatchNextPrograms.TYPE_TV_SERIES
					BaseItemKind.MOVIE -> WatchNextPrograms.TYPE_MOVIE
					BaseItemKind.EPISODE -> WatchNextPrograms.TYPE_TV_EPISODE
					BaseItemKind.AUDIO -> WatchNextPrograms.TYPE_TRACK
					BaseItemKind.PLAYLIST -> WatchNextPrograms.TYPE_PLAYLIST
					else -> WatchNextPrograms.TYPE_CHANNEL
				}
			)
			.setTitle(item.seriesName ?: item.name)
			.setEpisodeTitle(if (item.type == BaseItemKind.EPISODE) item.name else null)
			.setDescription(item.overview?.stripHtml())
			.setReleaseDate(
				if (item.premiereDate != null) DateTimeFormatter.ISO_DATE.format(item.premiereDate)
				else null
			)
			.setPosterArtUri(imageUri)
			.setPosterArtAspectRatio(
				when (item.type) {
					BaseItemKind.COLLECTION_FOLDER,
					BaseItemKind.EPISODE -> TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9

					else -> TvContractCompat.PreviewPrograms.ASPECT_RATIO_MOVIE_POSTER
				}
			)
			.setIntent(Intent(context, StartupActivity::class.java).apply {
				putExtra(StartupActivity.EXTRA_ITEM_ID, item.id.toString())
				putExtra(StartupActivity.EXTRA_ITEM_IS_USER_VIEW, item.type == BaseItemKind.COLLECTION_FOLDER)
			})
			.setDurationMillis(
				if (item.runTimeTicks?.ticks != null) {
					// If we are resuming, we need to show remaining time, cause GoogleTV
					// ignores setLastPlaybackPositionMillis
					val duration = item.runTimeTicks?.ticks ?: Duration.ZERO
					val playbackPosition = item.userData?.playbackPositionTicks?.ticks
						?: Duration.ZERO
					(duration - playbackPosition).inWholeMilliseconds.toInt()
				} else 0
			)
			.apply {
				if ((item.parentIndexNumber ?: 0) > 0)
					setSeasonNumber(seasonString, item.parentIndexNumber!!)
				if ((item.indexNumber ?: 0) > 0)
					setEpisodeNumber(episodeString, item.indexNumber!!)
			}.build().toContentValues()
	}

	/**
	 * Updates the "watch next" row with new and unfinished episodes. Does not include movies, music
	 * or other types of media. Uses the [nextUpItems] parameter to store items returned by a
	 * NextUpQuery().
	 */
	@SuppressLint("RestrictedApi")
	private fun updateWatchNext(nextUpItems: List<BaseItemDto>) {
		deletePrograms(nextUpItems)

		// Get current watch next state
		val currentWatchNextPrograms = getCurrentWatchNext()

		// Create all programs in nextUpItems but not in watch next
		val programsToAdd = nextUpItems
			.filter { next -> currentWatchNextPrograms.none { it.internalProviderId == next.id.toString() } }
		context.contentResolver.bulkInsert(
			WatchNextPrograms.CONTENT_URI,
			programsToAdd.map { item -> getBaseItemAsWatchNextProgram(item).toContentValues() }
				.toTypedArray())
	}

	/**
	 * Delete stale programs from the watch next row. Items that don't need to be touched are
	 * kept as is, so they keep their ordering in the watch next row.
	 */
	@SuppressLint("RestrictedApi")
	private fun deletePrograms(nextUpItems: List<BaseItemDto>) {
		// Retrieve current watch next row
		val currentWatchNextPrograms = getCurrentWatchNext()

		// Find all stale programs to delete
		val deletedByUser = currentWatchNextPrograms.filter { !it.isBrowsable }
		val noLongerInWatchNext =
			currentWatchNextPrograms.filter { (nextUpItems).none { next -> it.internalProviderId == next.id.toString() } }
		val continueWatching = currentWatchNextPrograms.filter { it.watchNextType == WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE }

		// Delete the programs
		(deletedByUser + noLongerInWatchNext + continueWatching)
			.forEach { context.contentResolver.delete(TvContractCompat.buildWatchNextProgramUri(it.id), null, null) }
	}

	/**
	 * Retrieves the current watch next row state.
	 */
	@SuppressLint("RestrictedApi")
	private fun getCurrentWatchNext(): MutableList<WatchNextProgram> {
		val currentWatchNextPrograms: MutableList<WatchNextProgram> = mutableListOf()
		context.contentResolver.query(WatchNextPrograms.CONTENT_URI, WatchNextProgram.PROJECTION, null, null, null)
			.use { cursor ->
				if (cursor != null && cursor.moveToFirst()) {
					do {
						currentWatchNextPrograms.add(WatchNextProgram.fromCursor(cursor))
					} while (cursor.moveToNext())
				}
			}
		return currentWatchNextPrograms
	}

	/**
	 * Convert [BaseItemDto] to [WatchNextProgram]. Assumes the item type is "episode".
	 */
	@Suppress("RestrictedApi")
	private fun getBaseItemAsWatchNextProgram(item: BaseItemDto) =
		WatchNextProgram.Builder().apply {
			val preferParentThumb = userPreferences[UserPreferences.seriesThumbnailsEnabled]

			setInternalProviderId(item.id.toString())

			// Poster size & type
			if (item.type == BaseItemKind.EPISODE) {
				setType(WatchNextPrograms.TYPE_TV_EPISODE)
				setPosterArtAspectRatio(WatchNextPrograms.ASPECT_RATIO_16_9)
			} else if (item.type == BaseItemKind.MOVIE) {
				setType(WatchNextPrograms.TYPE_MOVIE)
				setPosterArtAspectRatio(WatchNextPrograms.ASPECT_RATIO_MOVIE_POSTER)
			}

			// Name and episode details
			if (item.seriesName != null) {
				setTitle(item.seriesName)
				setEpisodeTitle(item.name)

				item.indexNumber?.takeIf { it > 0 }?.let { setEpisodeNumber(it) }
				item.parentIndexNumber?.takeIf { it > 0 }?.let { setSeasonNumber(it) }
			} else {
				setTitle(item.name)
			}

			setDescription(item.overview?.stripHtml())

			// Poster
			setPosterArtUri(item.getPosterArtImageUrl(preferParentThumb))

			when {
				// User has started playing the episode
				(item.userData?.playbackPositionTicks ?: 0) > 0 -> {
					setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
					setLastPlaybackPositionMillis(item.userData!!.playbackPositionTicks.ticks.inWholeMilliseconds.toInt())
					// Use last played date to prioritize

					setLastEngagementTimeUtcMillis(
						item.userData?.lastPlayedDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
							?: Instant.now().toEpochMilli()
					)
				}
				// First episode of the season
				item.indexNumber == 1 -> {
					setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_NEW)
					setLastEngagementTimeUtcMillis(
						item.dateCreated?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
							?: Instant.now().toEpochMilli()
					)
				}
				// Default
				else -> {
					setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_NEXT)
					setLastEngagementTimeUtcMillis(Instant.now().toEpochMilli())
				}
			}

			// Runtime has been determined
			item.runTimeTicks?.ticks?.let { setDurationMillis(it.inWholeMilliseconds.toInt()) }

			// Set intent to open the episode
			setIntent(Intent(context, StartupActivity::class.java).apply {
				putExtra(StartupActivity.EXTRA_ITEM_ID, item.id.toString())
			})
		}.build()
}
