package org.jellyfin.androidtv.ui.playback.external

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.activity.result.ActivityResult

interface ExternalPlayerApi {
	/**
	 * Check whether this player API supports a given app or not. Recommended is to check the package name of the given app.
	 */
	fun supports(app: ApplicationInfo): Boolean

	/**
	 * Populate the given [intent] with extras from [data] in the format requested by the external app.
	 */
	fun populateIntent(intent: Intent, data: ExternalPlayData)

	/**
	 * Read the [result] from the external app.
	 */
	fun parseResult(result: ActivityResult): ExternalPlayResult
}
