package org.jellyfin.androidtv.ui.playback.external

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.activity.result.ActivityResult
import kotlin.time.Duration.Companion.milliseconds

/**
 * Implementation of the MX Player API.
 * Documentation: https://mx.j2inter.com/api
 */
class MxExternalPlayerApi : ExternalPlayerApi {
	companion object {
		val PACKAGE_NAMES = arrayOf(
			"com.mxtech.videoplayer.ad",
		)

		private const val EXTRA_TITLE = "title"
		private const val EXTRA_POSITION = "position"
		private const val EXTRA_RETURN_RESULT = "return_result"
		private const val EXTRA_SUBS = "subs"
		private const val EXTRA_SUBS_NAME = "subs.name"

		private const val RESULT_EXTRA_POSITION = "position"
	}

	override fun supports(app: ApplicationInfo): Boolean = app.packageName in PACKAGE_NAMES

	override fun populateIntent(intent: Intent, data: ExternalPlayData) {
		intent.putExtra(EXTRA_TITLE, data.title)
		intent.putExtra(EXTRA_POSITION, data.position.inWholeMilliseconds.toInt())
		intent.putExtra(EXTRA_RETURN_RESULT, true)

		if (data.externalSubtitles.isNotEmpty()) {
			intent.putExtra(EXTRA_SUBS, data.externalSubtitles.map { it.url }.toTypedArray())
			intent.putExtra(EXTRA_SUBS_NAME, data.externalSubtitles.map { it.name }.toTypedArray())
		}
	}

	override fun parseResult(result: ActivityResult): ExternalPlayResult = when (result.resultCode) {
		Activity.RESULT_OK -> {
			// Try reading end position
			val position = result.data?.getIntExtra(RESULT_EXTRA_POSITION, -1)?.takeIf { it >= 0 }?.toLong()?.milliseconds

			ExternalPlayResult.Success(
				position = position,
			)
		}

		else -> ExternalPlayResult.Failed
	}
}
