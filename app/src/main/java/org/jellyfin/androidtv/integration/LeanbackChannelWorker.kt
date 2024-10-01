package org.jellyfin.androidtv.integration

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
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
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.integration.provider.ImageProvider
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.dp
import org.jellyfin.androidtv.util.sdk.isUsable
import org.jellyfin.androidtv.util.stripHtml
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.TimeoutException
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageFormat
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.extensions.ticks
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset
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
				Timber.d("Updating channel %s", channel)
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
	private fun getChannelUri(name: String, settings: Channel, default: Boolean = false): Uri {
		val store = context.getSharedPreferences("leanback_channels", Context.MODE_PRIVATE)

		val uri = if (store.contains(name)) {
			// Retrieve uri and update content resolver
			Uri.parse(store.getString(name, null)).also { uri ->
				context.contentResolver.update(uri, settings.toContentValues(), null, null)
			}
		} else {
			// Create new channel and save uri
			context.contentResolver.insert(
				TvContractCompat.Channels.CONTENT_URI,
				settings.toContentValues()
			)!!.also { uri ->
				store.edit().putString(name, uri.toString()).apply()
				if (default) {
					// Set as default row to display (we can request one row to automatically be added to the home screen)
					TvContractCompat.requestChannelBrowsable(context, ContentUris.parseId(uri))
				}
			}
		}

		// Update logo
		ResourcesCompat.getDrawable(context.resources, R.mipmap.app_icon, context.theme)?.let {
			ChannelLogoUtils.storeChannelLogo(
				context,
				ContentUris.parseId(uri),
				it.toBitmap(80.dp(context), 80.dp(context))
			)
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
		type == BaseItemKind.MOVIE || type == BaseItemKind.SERIES -> api.imageApi.getItemImageUrl(
			itemId = id,
			imageType = ImageType.PRIMARY,
			format = ImageFormat.WEBP,
			width = 106.dp(context),
			height = 153.dp(context),
			tag = imageTags?.get(ImageType.PRIMARY),
		)

		(preferParentThumb || imageTags?.contains(ImageType.PRIMARY) != true) && parentThumbItemId != null -> api.imageApi.getItemImageUrl(
			itemId = parentThumbItemId!!,
			imageType = ImageType.THUMB,
			format = ImageFormat.WEBP,
			width = 272.dp(context),
			height = 153.dp(context),
			tag = imageTags?.get(ImageType.THUMB),
		)

		imageTags?.containsKey(ImageType.PRIMARY) == true -> api.imageApi.getItemImageUrl(
			itemId = id,
			imageType = ImageType.PRIMARY,
			format = ImageFormat.WEBP,
			width = 272.dp(context),
			height = 153.dp(context),
			tag = imageTags?.get(ImageType.PRIMARY),
		)

		else -> imageHelper.getResourceUrl(context, R.drawable.tile_land_tv)
	}.let(ImageProvider::getImageUri)

	/**
	 * Gets the resume and next up episodes. The returned pair contains two lists:
	 * 1. resume items
	 * 2. next up items
	 */
	private suspend fun getNextUpItems(): Pair<List<BaseItemDto>, List<BaseItemDto>> =
		withContext(Dispatchers.IO) {
			val resume = async {
				api.itemsApi.getResumeItems(
					fields = listOf(ItemFields.DATE_CREATED),
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
					fields = listOf(ItemFields.DATE_CREATED),
				).content.items
			}

			// Concat
			Pair(resume.await(), nextUp.await())
		}

	private suspend fun getLatestMedia(): Triple<List<BaseItemDto>, List<BaseItemDto>, List<BaseItemDto>> =
		withContext(Dispatchers.IO) {
			val latestEpisodes = async {
				api.userLibraryApi.getLatestMedia(
					fields = listOf(
						ItemFields.OVERVIEW,
					),
					limit = 50,
					includeItemTypes = listOf(BaseItemKind.EPISODE),
					isPlayed = false
				).content
			}

			val latestMovies = async {
				api.userLibraryApi.getLatestMedia(
					fields = listOf(
						ItemFields.OVERVIEW,
					),
					limit = 50,
					includeItemTypes = listOf(BaseItemKind.MOVIE),
					isPlayed = false
				).content
			}

			val latestMedia = async {
				api.userLibraryApi.getLatestMedia(
					fields = listOf(
						ItemFields.OVERVIEW,
					),
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
			.setSeasonNumber(seasonString, item.parentIndexNumber ?: 0)
			.setEpisodeNumber(episodeString, item.indexNumber ?: 0)
			.setDescription(item.overview?.stripHtml())
			.setReleaseDate(
				if (item.premiereDate != null) DateTimeFormatter.ISO_DATE.format(item.premiereDate)
				else null
			)
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
			.build()
			.toContentValues()
	}

	/**
	 * Updates the "watch next" row with new and unfinished episodes. Does not include movies, music
	 * or other types of media. Uses the [nextUpItems] parameter to store items returned by a
	 * NextUpQuery().
	 */
	private fun updateWatchNext(nextUpItems: List<BaseItemDto>) {
		// Delete current items
		context.contentResolver.delete(WatchNextPrograms.CONTENT_URI, null, null)

		// Add new items
		context.contentResolver.bulkInsert(
			WatchNextPrograms.CONTENT_URI,
			nextUpItems.map { item -> getBaseItemAsWatchNextProgram(item).toContentValues() }
				.toTypedArray()
		)
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

			// Name
			if (item.seriesName != null) setTitle("${item.seriesName} - ${item.name}")
			else setTitle(item.name)

			// Poster
			setPosterArtUri(item.getPosterArtImageUrl(preferParentThumb))

			// Use date created or fallback to current time if unavailable
			var engagement = item.dateCreated

			when {
				// User has started playing the episode
				(item.userData?.playbackPositionTicks ?: 0) > 0 -> {
					setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
					setLastPlaybackPositionMillis(item.userData!!.playbackPositionTicks.ticks.inWholeMilliseconds.toInt())
					// Use last played date to prioritize
					engagement = item.userData?.lastPlayedDate
				}
				// First episode of the season
				item.indexNumber == 1 -> setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_NEW)
				// Default
				else -> setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_NEXT)
			}

			setLastEngagementTimeUtcMillis(
				engagement?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
					?: Instant.now().toEpochMilli()
			)

			// Episode runtime has been determined
			item.runTimeTicks?.let { runTimeTicks ->
				setDurationMillis(runTimeTicks.ticks.inWholeMilliseconds.toInt())
			}

			// Set intent to open the episode
			setIntent(Intent(context, StartupActivity::class.java).apply {
				putExtra(StartupActivity.EXTRA_ITEM_ID, item.id.toString())
			})
		}.build()
}
