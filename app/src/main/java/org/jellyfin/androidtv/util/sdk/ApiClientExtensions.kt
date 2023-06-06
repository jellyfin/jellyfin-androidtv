package org.jellyfin.androidtv.util.sdk

import org.jellyfin.sdk.api.client.ApiClient


/**
 * Check if the [baseUrl] and [accessToken] are not null.
 */
val ApiClient.isUsable
	get() = baseUrl != null && accessToken != null
