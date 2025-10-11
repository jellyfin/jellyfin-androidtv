package org.jellyfin.androidtv.ui.playback

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.profile.createDeviceProfile
import org.jellyfin.androidtv.util.sdk.getDisplayName
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.subtitleApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.extensions.inWholeTicks
import org.jellyfin.sdk.model.extensions.ticks
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Activity that, once opened, opens the first item of the [VideoQueueManager.getCurrentVideoQueue] list in an external media player app.
 * Once returned it will notify the server of item completion.
 */
class ExternalPlayerActivity : FragmentActivity() {
	companion object {
		const val EXTRA_POSITION = "position"

		// https://mx.j2inter.com/api
		private const val API_MX_TITLE = "title"
		private const val API_MX_SEEK_POSITION = "position"
		private const val API_MX_FILENAME = "filename"
		private const val API_MX_SECURE_URI = "secure_uri"
		private const val API_MX_RETURN_RESULT = "return_result"
		private const val API_MX_RESULT_POSITION = "position"
		private const val API_MX_SUBS = "subs"
		private const val API_MX_SUBS_NAME = "subs.name"
		private const val API_MX_SUBS_FILENAME = "subs.filename"

		// https://wiki.videolan.org/Android_Player_Intents/
		private const val API_VLC_SUBTITLES = "subtitles_location"
		private const val API_VLC_RESULT_POSITION = "extra_position"

		// https://www.vimu.tv/player-api
		private const val API_VIMU_TITLE = "forcename"
		private const val API_VIMU_SEEK_POSITION = "startfrom"
		private const val API_VIMU_RESUME = "forceresume"

		// The extra keys used by various video players to read the end position
		private val resultPositionExtras = arrayOf(API_MX_RESULT_POSITION, API_VLC_RESULT_POSITION)
	}

	private val videoQueueManager by inject<VideoQueueManager>()
	private val dataRefreshService by inject<DataRefreshService>()
	private val api by inject<ApiClient>()
	private val userPreferences by inject<UserPreferences>()
	private val serverRepository by inject<ServerRepository>()

	private data class PlaybackSessionInfo(
		val item: BaseItemDto,
		val mediaSource: MediaSourceInfo,
		val playSessionId: String?,
	)

	private val playVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		Timber.i("Playback finished with result code ${result.resultCode}")
		videoQueueManager.setCurrentMediaPosition(videoQueueManager.getCurrentMediaPosition() + 1)

		if (result.resultCode != RESULT_OK) {
			Toast.makeText(this, R.string.video_error_unknown_error, Toast.LENGTH_LONG).show()
			finish()
		} else {
			onItemFinished(result.data)
		}
	}

	private var currentItem: PlaybackSessionInfo? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val position = intent.getLongExtra(EXTRA_POSITION, 0).milliseconds
		playNext(position)
	}

	private fun playNext(position: Duration = Duration.ZERO) {
		val currentPosition = videoQueueManager.getCurrentMediaPosition()
		val item = videoQueueManager.getCurrentVideoQueue().getOrNull(currentPosition) ?: return finish()
		val mediaSource = item.mediaSources?.firstOrNull { it.id?.toUUIDOrNull() == item.id }

		if (mediaSource == null) {
			Toast.makeText(this, R.string.msg_no_playable_items, Toast.LENGTH_LONG).show()
			finish()
		} else {
			playItem(item, mediaSource, position)
		}
	}

	private suspend fun getPlaySessionId(item: BaseItemDto, mediaSource: MediaSourceInfo): String? {
		val profile = createDeviceProfile(
			this@ExternalPlayerActivity,
			userPreferences,
			serverRepository.currentServer.value?.serverVersion ?: org.jellyfin.sdk.model.ServerVersion(10, 9, 0)
		)
		val response by api.mediaInfoApi.getPostedPlaybackInfo(
			itemId = item.id,
			data = PlaybackInfoDto(
				mediaSourceId = mediaSource.id,
				deviceProfile = profile,
				// External players handle their own playback with direct files
				enableDirectPlay = true,
				enableDirectStream = false,
				enableTranscoding = false,
				allowVideoStreamCopy = false,
				allowAudioStreamCopy = false,
				autoOpenLiveStream = false,
			)
		)
		return response.playSessionId
	}

	private fun playItem(item: BaseItemDto, mediaSource: MediaSourceInfo, position: Duration) {
		lifecycleScope.launch {
			val playSessionId = getPlaySessionId(item, mediaSource)

			val url = api.videosApi.getVideoStreamUrl(
				itemId = item.id,
				mediaSourceId = mediaSource.id,
				static = true,
				playSessionId = playSessionId,
			)

			val title = item.getDisplayName(this@ExternalPlayerActivity)
			val fileName = mediaSource.path?.let { File(it).name }
			val externalSubtitles = mediaSource.mediaStreams
				?.filter { it.type == MediaStreamType.SUBTITLE && it.isExternal }
				?.sortedWith(compareBy<MediaStream> { it.isDefault }.thenBy { it.index })
				.orEmpty()

			val subtitleUrls = externalSubtitles.map {
				api.subtitleApi.getSubtitleUrl(
					routeItemId = item.id,
					routeMediaSourceId = mediaSource.id.toString(),
					routeIndex = it.index,
					routeFormat = it.codec.orEmpty(),
				)
			}.toTypedArray()
			val subtitleNames = externalSubtitles.map { it.displayTitle ?: it.title.orEmpty() }.toTypedArray()
			val subtitleLanguages = externalSubtitles.map { it.language.orEmpty() }.toTypedArray()

			Timber.i(
				"Starting item ${item.id} from $position with ${subtitleUrls.size} external subtitles: $url${
					subtitleUrls.joinToString(", ", ", ")
				}"
			)

			// Report playback start to server
			if (playSessionId != null) {
				reportPlaybackStart(item, mediaSource, playSessionId, position)
			}

			val playIntent = Intent(Intent.ACTION_VIEW).apply {
				val mediaType = when (item.mediaType) {
					MediaType.VIDEO -> "video/*"
					MediaType.AUDIO -> "audio/*"
					else -> null
				}

				setDataAndTypeAndNormalize(url.toUri(), mediaType)

				putExtra(API_MX_SEEK_POSITION, position.inWholeMilliseconds.toInt())
				putExtra(API_MX_RETURN_RESULT, true)
				putExtra(API_MX_TITLE, title)
				putExtra(API_MX_FILENAME, fileName)
				putExtra(API_MX_SECURE_URI, true)
				putExtra(API_MX_SUBS, subtitleUrls)
				putExtra(API_MX_SUBS_NAME, subtitleNames)
				putExtra(API_MX_SUBS_FILENAME, subtitleLanguages)

				if (subtitleUrls.isNotEmpty()) putExtra(API_VLC_SUBTITLES, subtitleUrls.first().toString())

				putExtra(API_VIMU_SEEK_POSITION, position.inWholeMilliseconds.toInt())
				putExtra(API_VIMU_RESUME, false)
				putExtra(API_VIMU_TITLE, title)
			}

			try {
				currentItem = PlaybackSessionInfo(item, mediaSource, playSessionId)
				playVideoLauncher.launch(playIntent)
			} catch (_: ActivityNotFoundException) {
				Toast.makeText(this@ExternalPlayerActivity, R.string.no_player_message, Toast.LENGTH_LONG).show()
				finish()
			}
		}
	}

	private fun reportPlaybackStart(
		item: BaseItemDto,
		mediaSource: MediaSourceInfo,
		playSessionId: String,
		position: Duration
	) {
		lifecycleScope.launch {
			runCatching {
				withContext(Dispatchers.IO) {
					api.playStateApi.reportPlaybackStart(
						PlaybackStartInfo(
							itemId = item.id,
							mediaSourceId = mediaSource.id,
							playSessionId = playSessionId,
							positionTicks = position.inWholeTicks,
							playMethod = PlayMethod.DIRECT_PLAY,
							canSeek = (item.runTimeTicks ?: 0) > 0,
							isPaused = false,
							isMuted = false,
							volumeLevel = 100,
							audioStreamIndex = mediaSource.defaultAudioStreamIndex,
							subtitleStreamIndex = mediaSource.defaultSubtitleStreamIndex,
							repeatMode = org.jellyfin.sdk.model.api.RepeatMode.REPEAT_NONE,
							playbackOrder = org.jellyfin.sdk.model.api.PlaybackOrder.DEFAULT,
						)
					)
				}
			}.onFailure { error ->
				Timber.w(error, "Failed to report playback start event")
			}
		}
	}


	private fun onItemFinished(result: Intent?) {
		if (currentItem == null) {
			Toast.makeText(this@ExternalPlayerActivity, R.string.video_error_unknown_error, Toast.LENGTH_LONG).show()
			finish()
			return
		}

		val sessionInfo = currentItem!!
		val item = sessionInfo.item
		val mediaSource = sessionInfo.mediaSource
		val playSessionId = sessionInfo.playSessionId

		val extras = result?.extras ?: Bundle.EMPTY

		val endPosition = resultPositionExtras.firstNotNullOfOrNull { key ->
			@Suppress("DEPRECATION") val value = extras.get(key)
			if (value is Number) value.toLong().milliseconds
			else null
		}

		val runtime = (mediaSource.runTimeTicks ?: item.runTimeTicks)?.ticks
		val shouldPlayNext = runtime != null && endPosition != null && endPosition >= (runtime * 0.9)

		lifecycleScope.launch {
			runCatching {
				withContext(Dispatchers.IO) {
					api.playStateApi.reportPlaybackStopped(
						PlaybackStopInfo(
							itemId = item.id,
							mediaSourceId = mediaSource.id,
							playSessionId = playSessionId,
							positionTicks = endPosition?.inWholeTicks,
							failed = false,
						)
					)
				}
			}.onFailure { error ->
				Timber.w(error, "Failed to report playback stop event")
				Toast.makeText(this@ExternalPlayerActivity, R.string.video_error_unknown_error, Toast.LENGTH_LONG).show()
			}

			dataRefreshService.lastPlayback = Instant.now()
			when (item.type) {
				BaseItemKind.MOVIE -> dataRefreshService.lastMoviePlayback = Instant.now()
				BaseItemKind.EPISODE -> dataRefreshService.lastTvPlayback = Instant.now()
				else -> Unit
			}

			if (shouldPlayNext) playNext()
			else finish()
		}
	}
}
