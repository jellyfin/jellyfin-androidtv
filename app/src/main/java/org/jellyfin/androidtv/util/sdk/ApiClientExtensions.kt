package org.jellyfin.androidtv.util.sdk

import org.jellyfin.sdk.api.client.ApiClient


/**
 * Check if the [baseUrl], [accessToken] and [userId] are not null.
 */
val ApiClient.isUsable
	get() = baseUrl != null && accessToken != null && userId != null
