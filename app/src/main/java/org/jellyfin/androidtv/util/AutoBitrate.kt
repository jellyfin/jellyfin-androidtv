package org.jellyfin.androidtv.util

import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.extensions.detectBitrate
import timber.log.Timber

/**
 * Small utility class to store and automatically detect a preferred bitrate.
 */
class AutoBitrate(
	private val api: ApiClient
) {
	var bitrate: Long? = null
		private set

	suspend fun detect() {
		try {
			val measurement = api.mediaInfoApi.detectBitrate()
			bitrate = measurement.bitrate
			Timber.i("Auto bitrate set to: %d", bitrate)
		} catch (err: ApiClientException) {
			Timber.e(err, "Failed to detect bitrate")
		} catch (err: IllegalArgumentException) {
			// Session probably ended before the detection completed
			Timber.e(err, "Failed to detect bitrate")
		}
	}
}
