package org.jellyfin.androidtv.util

import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.apiclient.interaction.ApiClient
import timber.log.Timber

/**
 * Small utility class to store and automatically detect a preferred bitrate.
 */
class AutoBitrate(
	private val apiClient: ApiClient
) {
	var bitrate: Long? = null
		private set

	suspend fun detect() {
		bitrate = callApi<Long> { apiClient.detectBitrate(it) }
		Timber.i("Auto bitrate set to: %d", bitrate)
	}

	fun getOr(default: Long) = bitrate ?: default
}
