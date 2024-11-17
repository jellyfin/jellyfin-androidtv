package org.jellyfin.androidtv.ui.playback

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.util.sdk.getDisplayName
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.extensions.inWholeTicks
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * Activity that, once opened, opens the first item of the [VideoQueueManager.getCurrentVideoQueue] list in an external media player app.
 * Once returned it will notify the server of item completion.
 */
class ExternalPlayerActivity : FragmentActivity() {
	companion object {
		const val EXTRA_POSITION = "position"

		// https://sites.google.com/site/mxvpen/api
		private const val API_MX_TITLE = "title"
		private const val API_MX_SEEK_POSITION = "position"
		private const val API_MX_FILENAME = "filename"
		private const val API_MX_SECURE_URI = "secure_uri"
		private const val API_MX_RETURN_RESULT = "return_result"
		private const val API_MX_RESULT_POSITION = "position"

		// https://wiki.videolan.org/Android_Player_Intents/
		private const val API_VLC_FROM_START = "from_start"
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

	private val playVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		if (result.resultCode != RESULT_OK) {
			Toast.makeText(this, R.string.video_error_unknown_error, Toast.LENGTH_LONG).show()
		} else {
			onItemFinished(result.data)
		}

		finish()
	}

	private var currentItem: BaseItemDto? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val item = videoQueueManager.getCurrentVideoQueue().firstOrNull()
		val position = intent.getLongExtra(EXTRA_POSITION, 0)

		if (item == null) {
			Toast.makeText(this, R.string.msg_no_playable_items, Toast.LENGTH_LONG).show()
			finish()
		} else {
			videoQueueManager.clearVideoQueue()
			playItem(item, position)
		}
	}

	private fun playItem(item: BaseItemDto, position: Long) {
		val url = api.videosApi.getVideoStreamUrl(
			itemId = item.id,
			static = true,
		)

		val title = item.getDisplayName(this)
		val fileName = item.path?.let { File(it).name }

		Timber.i("Starting item ${item.id}: $url")
		val playIntent = Intent(Intent.ACTION_VIEW).apply {
			val mediaType = if (item.mediaType == MediaType.VIDEO) "video/*"
			else if (item.mediaType == MediaType.AUDIO) "audio/*"
			else null

			setDataAndTypeAndNormalize(url.toUri(), mediaType)

			putExtra(API_MX_SEEK_POSITION, position)
			putExtra(API_MX_RETURN_RESULT, true)
			putExtra(API_MX_TITLE, title)
			putExtra(API_MX_FILENAME, fileName)
			putExtra(API_MX_SECURE_URI, true)

			putExtra(API_VLC_FROM_START, true)

			putExtra(API_VIMU_SEEK_POSITION, position)
			putExtra(API_VIMU_RESUME, false)
			putExtra(API_VIMU_TITLE, title)
		}

		try {
			currentItem = item
			playVideoLauncher.launch(playIntent)
		} catch (_: ActivityNotFoundException) {
			Toast.makeText(this, R.string.no_player_message, Toast.LENGTH_LONG).show()
			finish()
		}
	}


	private fun onItemFinished(result: Intent?) {
		val item = currentItem
		if (item == null) {
			Toast.makeText(this@ExternalPlayerActivity, R.string.video_error_unknown_error, Toast.LENGTH_LONG).show()
			finish()
			return
		}
		val extras = result?.extras

		val endPosition = if (extras == null) null else resultPositionExtras
			.firstOrNull { extra -> extras.containsKey(extra) == true }
			?.let { extra -> extras.getInt(extra, 0).milliseconds }

		lifecycleScope.launch {
			runCatching {
				api.playStateApi.reportPlaybackStopped(
					PlaybackStopInfo(
						itemId = item.id,
						positionTicks = endPosition?.inWholeTicks,
						failed = false,
					)
				)
			}.onFailure {
				Toast.makeText(this@ExternalPlayerActivity, R.string.video_error_unknown_error, Toast.LENGTH_LONG).show()
			}

			dataRefreshService.lastPlayback = Instant.now()
			when (item.type) {
				BaseItemKind.MOVIE -> dataRefreshService.lastMoviePlayback = Instant.now()
				BaseItemKind.EPISODE -> dataRefreshService.lastTvPlayback = Instant.now()
				else -> Unit
			}

			finish()
		}
	}
}
