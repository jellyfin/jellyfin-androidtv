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
		private const val EXTRA_SUBS_ENABLE = "subs.enable"
		private const val EXTRA_SECURE_URI = "secure_uri"
		private const val EXTRA_FILENAME = "filename"

		private const val RESULT_EXTRA_POSITION = "position"
		private const val RESULT_EXTRA_END_BY = "end_by"
		private const val RESULT_END_BY_USER = "user"
		private const val RESULT_END_BY_PLAYBACK_COMPLETION = "playback_completion"
	}

	override fun supports(app: ApplicationInfo): Boolean = app.packageName in PACKAGE_NAMES

	override fun populateIntent(intent: Intent, data: ExternalPlayData) {
		intent.putExtra(EXTRA_TITLE, data.title)
		intent.putExtra(EXTRA_POSITION, data.position.inWholeMilliseconds.toInt())
		intent.putExtra(EXTRA_RETURN_RESULT, true)
		intent.putExtra(EXTRA_SECURE_URI, true)

		data.fileName?.let { intent.putExtra(EXTRA_FILENAME, it) }

		if (data.externalSubtitles.isNotEmpty()) {
			intent.putExtra(EXTRA_SUBS, data.externalSubtitles.map { it.url }.toTypedArray())
			intent.putExtra(EXTRA_SUBS_NAME, data.externalSubtitles.map { it.name }.toTypedArray())

			// Select the default subtitles, must be a subset of the subtitles added above
			val defaultSubtitles = data.externalSubtitles.filter { it.mediaStream.isDefault }
			if (defaultSubtitles.isNotEmpty()) {
				intent.putExtra(EXTRA_SUBS_ENABLE, defaultSubtitles.map { it.url }.toTypedArray())
			}
		}
	}

	override fun parseResult(result: ActivityResult): ExternalPlayResult = when (result.resultCode) {
		Activity.RESULT_OK -> {
			val position = result.data?.getIntExtra(RESULT_EXTRA_POSITION, -1)?.takeIf { it >= 0 }?.toLong()?.milliseconds
			val completed = when (result.data?.getStringExtra(RESULT_EXTRA_END_BY)) {
				RESULT_END_BY_PLAYBACK_COMPLETION -> true
				RESULT_END_BY_USER -> false
				else -> null
			}

			ExternalPlayResult.Success(
				position = position,
				completed = completed,
			)
		}

		else -> ExternalPlayResult.Failed
	}
}
