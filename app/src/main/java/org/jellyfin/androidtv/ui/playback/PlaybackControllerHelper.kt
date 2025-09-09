package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.util.sdk.end
import org.jellyfin.androidtv.util.sdk.start
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.UUID

fun PlaybackController.getLiveTvChannel(
	id: UUID,
	callback: (channel: BaseItemDto) -> Unit,
) {
	val api by fragment.inject<ApiClient>()

	fragment.lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getChannel(id).content
			}
		}.onSuccess { channel ->
			callback(channel)
		}
	}
}

@OptIn(UnstableApi::class)
fun PlaybackController.disableDefaultSubtitles() {
	Timber.i("Disabling non-baked subtitles")

	with(mVideoManager.mExoPlayer.trackSelector!!) {
		parameters = parameters.buildUpon()
			.clearOverridesOfType(C.TRACK_TYPE_TEXT)
			.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
			.build()
	}
}

@OptIn(UnstableApi::class)
@JvmOverloads
fun PlaybackController.setSubtitleIndex(index: Int, force: Boolean = false) {
	Timber.i("Switching subtitles from index ${mCurrentOptions.subtitleStreamIndex} to $index")

	// Already using this subtitle index
	if (mCurrentOptions.subtitleStreamIndex == index && !force) return

	// Disable subtitles
	if (index == -1) {
		mCurrentOptions.subtitleStreamIndex = -1

		if (burningSubs) {
			Timber.i("Disabling subtitle baking")

			stop()
			burningSubs = false
			play(mCurrentPosition, -1)
		} else {
			disableDefaultSubtitles()
		}
	} else if (burningSubs) {
		Timber.i("Restarting playback to disable subtitle baking")

		// If we're currently burning subs and want to switch streams we need some special behavior
		// to stop the current baked subs. We can just stop & start with the new subtitle index for that
		stop()
		burningSubs = false
		mCurrentOptions.subtitleStreamIndex = index
		play(mCurrentPosition, index)
	} else {
		val mediaSource = currentMediaSource
		val stream = mediaSource.mediaStreams?.firstOrNull { it.type == MediaStreamType.SUBTITLE && it.index == index }
		if (stream == null) {
			Timber.w("Failed to find correct media stream")
			return setSubtitleIndex(-1)
		}

		when (stream.deliveryMethod) {
			SubtitleDeliveryMethod.ENCODE -> {
				Timber.i("Restarting playback for subtitle baking")

				stop()
				burningSubs = true
				mCurrentOptions.subtitleStreamIndex = index
				play(mCurrentPosition, index)
			}

			SubtitleDeliveryMethod.EXTERNAL,
			SubtitleDeliveryMethod.EMBED,
			SubtitleDeliveryMethod.HLS -> {
				// External subtitles need to be resolved differently
				val group = if (stream.deliveryMethod == SubtitleDeliveryMethod.EXTERNAL) {
					mVideoManager.mExoPlayer.currentTracks.groups.firstOrNull { group ->
						// Verify this is a group with a single format (the subtitles) that is added by us. Because ExoPlayer uses a
						// MergingMediaSource, each external subtitle format id is prefixed with its source index (normally starting at 1,
						// increasing for each external subttitle). So we only check the end of the id
						group.length == 1 && group.getTrackFormat(0).id?.endsWith(":JF_EXTERNAL:$index") == true
					}
				} else {
					// The server does not send a reliable index in all cases, so calculate it manually
					val localIndex = mediaSource.mediaStreams.orEmpty()
						.filter { it.type == MediaStreamType.SUBTITLE }
						.filter { it.deliveryMethod == SubtitleDeliveryMethod.EMBED || it.deliveryMethod == SubtitleDeliveryMethod.HLS }
						.indexOf(stream)
						.takeIf { it != -1 }

					if (localIndex == null) {
						Timber.w("Failed to find local subtitle index")
						return setSubtitleIndex(-1)
					}

					mVideoManager.mExoPlayer.currentTracks.groups
						.filter { it.type == C.TRACK_TYPE_TEXT }
						.filterNot { it.length == 1 && it.getTrackFormat(0).id?.endsWith(":JF_EXTERNAL:$index") == true }
						.getOrNull(localIndex)
				}?.mediaTrackGroup

				if (group == null) {
					Timber.w("Failed to find correct subtitle group for method ${stream.deliveryMethod}")
					return setSubtitleIndex(-1)
				}

				Timber.i("Enabling subtitle group $index via method ${stream.deliveryMethod}")
				mCurrentOptions.subtitleStreamIndex = index
				with(mVideoManager.mExoPlayer.trackSelector!!) {
					parameters = parameters.buildUpon()
						.clearOverridesOfType(C.TRACK_TYPE_TEXT)
						.addOverride(TrackSelectionOverride(group, 0))
						.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
						.build()
				}
			}

			SubtitleDeliveryMethod.DROP, null -> {
				Timber.i("Dropping subtitles")
				setSubtitleIndex(-1)
			}
		}
	}
}

fun PlaybackController.applyMediaSegments(
	item: BaseItemDto,
	callback: () -> Unit,
) {
	val mediaSegmentRepository by fragment.inject<MediaSegmentRepository>()

	fragment?.clearSkipOverlay()
	fragment.lifecycleScope.launch {
		val mediaSegments = runCatching {
			mediaSegmentRepository.getSegmentsForItem(item)
		}.getOrNull().orEmpty()

		for (mediaSegment in mediaSegments) {
			val action = mediaSegmentRepository.getMediaSegmentAction(mediaSegment)

			when (action) {
				MediaSegmentAction.SKIP -> addSkipAction(mediaSegment)
				MediaSegmentAction.ASK_TO_SKIP -> addAskToSkipAction(mediaSegment)
				MediaSegmentAction.NOTHING -> Unit
			}
		}

		callback()
	}
}

@OptIn(UnstableApi::class)
private fun PlaybackController.addSkipAction(mediaSegment: MediaSegmentDto) {
	mVideoManager.mExoPlayer
		.createMessage { _, _ ->
			// We can't seek directly on the ExoPlayer instance as not all media is seekable
			// the seek function in the PlaybackController checks this and optionally starts a transcode
			// at the requested position
			fragment.lifecycleScope.launch(Dispatchers.Main) {
				seek(mediaSegment.end.inWholeMilliseconds, true)
			}
		}
		// Segments at position 0 will never be hit by ExoPlayer so we need to add a minimum value
		.setPosition(mediaSegment.start.inWholeMilliseconds.coerceAtLeast(1))
		.setDeleteAfterDelivery(false)
		.send()
}

@OptIn(UnstableApi::class)
private fun PlaybackController.addAskToSkipAction(mediaSegment: MediaSegmentDto) {
	mVideoManager.mExoPlayer
		.createMessage { _, _ ->
			fragment?.askToSkip(mediaSegment.end)
		}
		// Segments at position 0 will never be hit by ExoPlayer so we need to add a minimum value
		.setPosition(mediaSegment.start.inWholeMilliseconds.coerceAtLeast(1))
		.setDeleteAfterDelivery(false)
		.send()
}
