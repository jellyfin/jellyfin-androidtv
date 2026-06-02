package org.jellyfin.androidtv.ui.playback.external

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.activity.result.ActivityResult
import kotlin.time.Duration.Companion.milliseconds

/**
 * Implementation of the VLC video player API.
 * Documentation: https://wiki.videolan.org/Android_Player_Intents/
 */
class VlcExternalPlayerApi : ExternalPlayerApi {
	companion object {
		val PACKAGE_NAMES = arrayOf(
			"org.videolan.vlc"
		)

		private const val EXTRA_SUBTITLES_LOCATION = "subtitles_location"
		private const val EXTRA_TITLE = "title"
		private const val EXTRA_POSITION = "position"

		private const val RESULT_CODE_OK = -1
		private const val RESULT_CODE_CANCELED = 0
		private const val RESULT_CODE_CONNECTION_FAILED = 2
		private const val RESULT_CODE_PLAYBACK_ERROR = 3
		private const val RESULT_CODE_HARDWARE_ACCELERATION_ERROR = 4
		private const val RESULT_CODE_VIDEO_TRACK_LOST = 5

		private const val RESULT_EXTRA_POSITION = "extra_position"
	}

	override fun supports(app: ApplicationInfo): Boolean = app.packageName in PACKAGE_NAMES

	override fun populateIntent(intent: Intent, data: ExternalPlayData) {
		intent.putExtra(EXTRA_TITLE, data.title)
		intent.putExtra(EXTRA_POSITION, data.position.inWholeMilliseconds.toInt())

		// Just a single external subtitle can be added
		if (data.externalSubtitles.isNotEmpty()) {
			intent.putExtra(EXTRA_SUBTITLES_LOCATION, data.externalSubtitles.first().url.toString())
		}
	}

	override fun parseResult(result: ActivityResult): ExternalPlayResult = when (result.resultCode) {
		RESULT_CODE_OK -> {
			// Try reading end position
			val position = result.data?.getLongExtra(RESULT_EXTRA_POSITION, -1L)?.takeIf { it > 0L }?.milliseconds

			ExternalPlayResult.Success(
				position = position,
			)
		}

		else -> ExternalPlayResult.Failed
	}
}
