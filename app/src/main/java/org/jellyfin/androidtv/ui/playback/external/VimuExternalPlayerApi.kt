package org.jellyfin.androidtv.ui.playback.external

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.activity.result.ActivityResult
import kotlin.time.Duration.Companion.milliseconds

/**
 * Implementation of the VIMU Media Player API.
 * Documentation: https://www.vimu.tv/player-api
 */
class VimuExternalPlayerApi : ExternalPlayerApi {
	companion object {
		val PACKAGE_NAMES = arrayOf(
			"net.gtvbox.videoplayer",
		)

		private const val EXTRA_TITLE = "forcename"
		private const val EXTRA_POSITION = "startfrom"
		private const val EXTRA_SUBTITLE_URL = "forcedsrt"

		private const val RESULT_CODE_COMPLETED = 1
		private const val RESULT_CODE_INTERRUPTED = 0

		private const val RESULT_EXTRA_POSITION = "position"
	}

	override fun supports(app: ApplicationInfo): Boolean = app.packageName in PACKAGE_NAMES

	override fun populateIntent(intent: Intent, data: ExternalPlayData) {
		intent.putExtra(EXTRA_TITLE, data.title)
		intent.putExtra(EXTRA_POSITION, data.position.inWholeMilliseconds.toInt())

		// Just a single external subtitle can be added
		if (data.externalSubtitles.isNotEmpty()) {
			intent.putExtra(EXTRA_SUBTITLE_URL, data.externalSubtitles.first().url.toString())
		}
	}

	override fun parseResult(result: ActivityResult): ExternalPlayResult = when (result.resultCode) {
		RESULT_CODE_COMPLETED -> ExternalPlayResult.Success(
			completed = true,
		)

		RESULT_CODE_INTERRUPTED -> {
			// Try reading end position
			val position = result.data?.getIntExtra(RESULT_EXTRA_POSITION, -1)?.takeIf { it >= 0 }?.toLong()?.milliseconds

			ExternalPlayResult.Success(
				completed = false,
				position = position,
			)
		}

		else -> ExternalPlayResult.Failed
	}
}
