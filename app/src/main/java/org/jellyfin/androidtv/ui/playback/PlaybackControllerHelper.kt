package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.extensions.ticks
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

fun PlaybackController.applyMediaSegments(
	item: BaseItemDto,
	callback: () -> Unit,
) {
	val mediaSegmentRepository by fragment.inject<MediaSegmentRepository>()

	fragment.lifecycleScope.launch {
		val mediaSegments = runCatching {
			mediaSegmentRepository.getSegmentsForItem(item)
		}.getOrNull().orEmpty()

		for (mediaSegment in mediaSegments) {
			val action = mediaSegmentRepository.getMediaSegmentAction(mediaSegment)

			when (action) {
				MediaSegmentAction.SKIP -> addSkipAction(mediaSegment)
				MediaSegmentAction.NOTHING -> Unit
			}
		}

		callback()
	}
}

@OptIn(UnstableApi::class)
private fun PlaybackController.addSkipAction(mediaSegment: MediaSegmentDto) {
	mVideoManager.mExoPlayer
		.createMessage { messageType: Int, payload: Any? ->
			// We can't seek directly on the ExoPlayer instance as not all media is seekable
			// the seek function in the PlaybackController checks this and optionally starts a transcode
			// at the requested position
			fragment.lifecycleScope.launch(Dispatchers.Main) {
				seek(mediaSegment.endTicks.ticks.inWholeMilliseconds)
			}
		}
		// Segments at position 0 will never be hit by ExoPlayer so we need to add a minimum value
		.setPosition(mediaSegment.startTicks.ticks.inWholeMilliseconds.coerceAtLeast(1))
		.setPayload(mediaSegment)
		.setDeleteAfterDelivery(false)
		.send()
}
