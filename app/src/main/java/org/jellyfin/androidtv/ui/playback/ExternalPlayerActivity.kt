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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.ExternalAppRepository
import org.jellyfin.androidtv.ui.playback.external.ExternalPlayData
import org.jellyfin.androidtv.ui.playback.external.ExternalPlayResult
import org.jellyfin.androidtv.ui.playback.external.ExternalPlayerApi
import org.jellyfin.androidtv.util.componentName
import org.jellyfin.androidtv.util.sdk.getDisplayName
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.subtitleApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlaybackStopInfo
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
	}

	private var currentPlayer: ExternalPlayerApi? = null

	private val videoQueueManager by inject<VideoQueueManager>()
	private val dataRefreshService by inject<DataRefreshService>()
	private val externalAppRepository by inject<ExternalAppRepository>()
	private val api by inject<ApiClient>()

	private var resultJob: Job? = null

	private val playVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		Timber.i("Playback finished with result code ${result.resultCode}")

		val player = currentPlayer ?: externalAppRepository.defaultExternalPlayerApi
		val playResult = player.parseResult(result)

		resultJob = lifecycleScope.launch {
			when (playResult) {
				is ExternalPlayResult.Success -> onPlayResultSuccess(playResult)
				is ExternalPlayResult.Failed -> onPlayResultFailed()
			}
		}
		// Deregister on complete
		resultJob?.invokeOnCompletion { resultJob = null }
	}

	private var currentItem: Pair<BaseItemDto, MediaSourceInfo>? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		lifecycleScope.launch {
			// Wait for result job to complete before attempting to do anything
			resultJob?.join()

			// If we're not finishing we should be fine to launch the initial playback
			if (!isFinishing) {
				val position = intent.getLongExtra(EXTRA_POSITION, 0).milliseconds
				playNext(position)
			}
		}
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

	private fun playItem(item: BaseItemDto, mediaSource: MediaSourceInfo, position: Duration) {
		val videoUrl = api.videosApi.getVideoStreamUrl(
			itemId = item.id,
			mediaSourceId = mediaSource.id,
			static = true,
		).toUri()

		val playIntent = Intent(Intent.ACTION_VIEW).apply {
			val mediaType = when (item.mediaType) {
				MediaType.VIDEO -> "video/*"
				MediaType.AUDIO -> "audio/*"
				else -> null
			}

			// Set configured app to launch
			externalAppRepository
				.getCurrentExternalPlayerApp(this@ExternalPlayerActivity)
				?.componentName
				?.let(::setComponent)

			setDataAndTypeAndNormalize(videoUrl, mediaType)
		}

		val resolveInfo = packageManager.queryIntentActivities(playIntent, 0).firstOrNull()
		if (resolveInfo == null) {
			Toast.makeText(this, R.string.no_player_message, Toast.LENGTH_LONG).show()
			finish()
			return
		}

		// Find player implementation to use
		val player = externalAppRepository.getExternalPlayerApi(resolveInfo.activityInfo)

		// Create play data and assign
		val playData = ExternalPlayData(
			url = videoUrl,
			title = item.getDisplayName(this),
			fileName = mediaSource.path?.let { File(it).name },
			externalSubtitles = mediaSource.mediaStreams
				?.filter { it.type == MediaStreamType.SUBTITLE && it.isExternal }
				?.sortedWith(compareBy<MediaStream> { it.isDefault }.thenBy { it.index })
				.orEmpty()
				.map { mediaStream ->
					// We cannot use the DeliveryUrl as that is only populated when using the playback info API, which we skip as we'll
					// always direct play when using external players. We need to infer the subtitle format based on its path (similar to
					// how the server calculates it)
					val format = mediaStream.path?.substringAfterLast('.', missingDelimiterValue = mediaStream.codec.orEmpty()) ?: "srt"
					val url = api.subtitleApi.getSubtitleUrl(
						routeItemId = item.id,
						routeMediaSourceId = mediaSource.id.toString(),
						routeIndex = mediaStream.index,
						routeFormat = format,
					).toUri()

					ExternalPlayData.Subtitle(
						mediaStream = mediaStream,
						url = url,
						name = mediaStream.displayTitle ?: mediaStream.title,
						language = mediaStream.language
					)
				},
			position = position,
		)
		player.populateIntent(playIntent, playData)

		Timber.i("Starting item ${item.id} from $position using $player. playData=$playData")

		// Launch playback
		try {
			currentPlayer = player
			currentItem = item to mediaSource
			playVideoLauncher.launch(playIntent)
		} catch (_: ActivityNotFoundException) {
			Toast.makeText(this, R.string.no_player_message, Toast.LENGTH_LONG).show()
			finish()
		}
	}

	private fun onPlayResultFailed() {
		Toast.makeText(this, R.string.video_error_unknown_error, Toast.LENGTH_LONG).show()
		finish()
	}

	private suspend fun onPlayResultSuccess(playResult: ExternalPlayResult.Success) {
		// Advance queue
		videoQueueManager.setCurrentMediaPosition(videoQueueManager.getCurrentMediaPosition() + 1)

		// Check cache if we have an item to report on
		if (currentItem == null) {
			Toast.makeText(this@ExternalPlayerActivity, R.string.video_error_unknown_error, Toast.LENGTH_LONG).show()
			finish()
			return
		}

		val (item, mediaSource) = currentItem!!
		val runtime = mediaSource.runTimeTicks?.ticks ?: item.runTimeTicks?.ticks

		// Determine end position for reporting
		val endPosition = when {
			// Use supplied position if set
			playResult.position != null -> playResult.position
			// Use runtime as fallback if completed
			playResult.completed == true -> runtime

			// Omit position if none is given and no completion signal is given
			else -> null
		}

		// Determine if the next queue item should be played
		val shouldPlayNext = when {
			playResult.completed != null -> playResult.completed
			runtime != null && endPosition != null -> endPosition >= (runtime * 0.9)
			else -> false
		}

		// Report playback event
		runCatching {
			withContext(Dispatchers.IO) {
				api.playStateApi.reportPlaybackStopped(
					PlaybackStopInfo(
						itemId = item.id,
						mediaSourceId = mediaSource.id,
						positionTicks = endPosition?.inWholeTicks,
						failed = false,
					)
				)
			}
		}.onFailure { error ->
			Timber.w(error, "Failed to report playback stop event")
			Toast.makeText(this@ExternalPlayerActivity, R.string.video_error_unknown_error, Toast.LENGTH_LONG).show()
		}

		// Update data refresh service
		dataRefreshService.lastPlayback = Instant.now()
		when (item.type) {
			BaseItemKind.MOVIE -> dataRefreshService.lastMoviePlayback = Instant.now()
			BaseItemKind.EPISODE -> dataRefreshService.lastTvPlayback = Instant.now()
			else -> Unit
		}

		// Act on
		if (shouldPlayNext) playNext()
		else finish()
	}
}
