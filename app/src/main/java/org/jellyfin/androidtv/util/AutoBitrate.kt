package org.jellyfin.androidtv.util

import org.jellyfin.sdk.api.client.KtorClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.detectBitrate
import org.jellyfin.sdk.api.operations.MediaInfoApi
import timber.log.Timber

/**
 * Small utility class to store and automatically detect a preferred bitrate.
 */
class AutoBitrate(
	private val apiClient: KtorClient
) {
	private val mediaInfoApi = MediaInfoApi(apiClient)

	var bitrate: Long? = null
		private set

	suspend fun detect() {
		try {
			val measurement = mediaInfoApi.detectBitrate()
			bitrate = measurement.bitrate
			Timber.i("Auto bitrate set to: %d", bitrate)
		} catch (err: ApiClientException) {
			Timber.e(err, "Failed to detect bitrate")
		}
	}
}
