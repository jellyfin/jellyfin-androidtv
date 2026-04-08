package org.jellyfin.androidtv.ui.player.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.androidtv.ui.playback.overlay.PlaybackInfoContent
import org.jellyfin.androidtv.ui.playback.overlay.PlaybackInfoData
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.mediaStreamFlow
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.model.api.PlayMethod

@Composable
fun PlaybackInfoOverlay(
	playbackManager: PlaybackManager,
	modifier: Modifier = Modifier,
) {
	val entry by rememberQueueEntry(playbackManager)
	val mediaStream by entry?.mediaStreamFlow?.collectAsState(null) ?: return
	val stream = mediaStream ?: return
	val item = entry?.baseItem

	val playMethod = when (stream.conversionMethod) {
		MediaConversionMethod.None -> PlayMethod.DIRECT_PLAY
		MediaConversionMethod.Remux -> PlayMethod.DIRECT_STREAM
		MediaConversionMethod.Transcode -> PlayMethod.TRANSCODE
	}

	val mediaSource = item?.mediaSources?.firstOrNull()

	val mbitStr = stringResource(R.string.bitrate_mbit, 0f).substringAfter("0")
	val kbitStr = stringResource(R.string.bitrate_kbit, 0f).substringAfter("0")

	val data = PlaybackInfoData.from(
		playMethod = playMethod,
		container = stream.container.format,
		mediaSource = mediaSource,
		formatBitrate = { bitrate ->
			when {
				bitrate >= 1_000_000 -> "%.1f$mbitStr".format(bitrate / 1_000_000f)
				else -> "%.0f$kbitStr".format(bitrate / 1_000f)
			}
		}
	)

	PlaybackInfoContent(
		data = data,
		modifier = modifier,
	)
}
