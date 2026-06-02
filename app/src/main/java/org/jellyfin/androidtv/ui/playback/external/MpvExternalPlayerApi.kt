package org.jellyfin.androidtv.ui.playback.external

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.activity.result.ActivityResult
import kotlin.time.Duration.Companion.milliseconds

/**
 * Implementation of the MPV player API.
 * Documentation: https://mpv-android.github.io/mpv-android/intent.html
 */
class MpvExternalPlayerApi : ExternalPlayerApi {
	companion object {
		val PACKAGE_NAMES = arrayOf(
			"is.xyz.mpv"
		)

		private const val EXTRA_TITLE = "media-title"
		private const val EXTRA_POSITION = "position"
		private const val EXTRA_SUBS = "subs"

		private const val RESULT_EXTRA_POSITION = "position"
	}

	override fun supports(app: ApplicationInfo): Boolean = app.packageName in PACKAGE_NAMES

	override fun populateIntent(intent: Intent, data: ExternalPlayData) {
		intent.putExtra(EXTRA_TITLE, data.title)
		intent.putExtra(EXTRA_POSITION, data.position.inWholeMilliseconds.toInt())

		if (data.externalSubtitles.isNotEmpty()) {
			intent.putExtra(EXTRA_SUBS, data.externalSubtitles.map { it.url }.toTypedArray())
		}
	}

	override fun parseResult(result: ActivityResult): ExternalPlayResult = when (result.resultCode) {
		Activity.RESULT_OK -> {
			// If playback completed to the end the position is omitted
			val completed = result.data?.hasExtra(RESULT_EXTRA_POSITION) == false
			val position = result.data?.getIntExtra(RESULT_EXTRA_POSITION, -1)?.takeIf { it >= 0 }?.toLong()?.milliseconds

			ExternalPlayResult.Success(
				completed = completed,
				position = position,
			)
		}

		else -> ExternalPlayResult.Failed
	}
}
