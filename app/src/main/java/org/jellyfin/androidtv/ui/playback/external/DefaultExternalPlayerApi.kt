package org.jellyfin.androidtv.ui.playback.external

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.activity.result.ActivityResult

/**
 * Default external video player used as fallback when no known API is available.
 * This implementation only supports basic extras that are broadly available in external players has a simple parser for
 * activity results. Handling for specific players should be done in dedicated implementations.
 */
class DefaultExternalPlayerApi : ExternalPlayerApi {
	companion object {
		private const val EXTRA_TITLE = "title"
		private const val EXTRA_POSITION = "position"

		private const val RESULT_CODE_OK = Activity.RESULT_OK
	}

	// Do not claim to support anything so this implementation will only be used as fallback.
	override fun supports(app: ApplicationInfo): Boolean = false

	override fun populateIntent(intent: Intent, data: ExternalPlayData) {
		intent.putExtra(EXTRA_TITLE, data.title)
		intent.putExtra(EXTRA_POSITION, data.position.inWholeMilliseconds.toInt())
	}

	override fun parseResult(result: ActivityResult): ExternalPlayResult = when (result.resultCode) {
		RESULT_CODE_OK -> ExternalPlayResult.Success()
		else -> ExternalPlayResult.Failed
	}
}
