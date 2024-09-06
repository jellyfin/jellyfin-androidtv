package org.jellyfin.androidtv.ui.playback

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.android.ext.android.inject
import java.util.UUID

fun PlaybackController.getLiveTvChannel(
	id: UUID,
	callback: (channel: BaseItemDto) -> Unit,
) {
	val api by fragment.inject<ApiClient>()

	fragment.lifecycleScope.launch {
		runCatching {
			api.liveTvApi.getChannel(id).content
		}.onSuccess { channel ->
			callback(channel)
		}
	}
}
