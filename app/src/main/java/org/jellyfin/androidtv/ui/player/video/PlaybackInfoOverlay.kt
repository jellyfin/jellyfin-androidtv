package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.playback.core.mediastream.mediaStreamFlow

@Composable
fun PlaybackInfoOverlay(
	playbackManager: PlaybackManager,
	modifier: Modifier = Modifier,
) {
	val entry by rememberQueueEntry(playbackManager)
	val mediaStream by entry?.mediaStreamFlow?.collectAsState(null) ?: return
	val stream = mediaStream ?: return

	val videoTrack = stream.tracks.filterIsInstance<MediaStreamVideoTrack>().firstOrNull()
	val audioTrack = stream.tracks.filterIsInstance<MediaStreamAudioTrack>().firstOrNull()

	Column(
		verticalArrangement = Arrangement.spacedBy(4.dp),
		modifier = modifier
			.background(Color.Black.copy(alpha = 0.8f))
			.padding(16.dp)
	) {
		// Play method
		val methodStr = when (stream.conversionMethod) {
			MediaConversionMethod.None -> stringResource(R.string.playback_info_direct_play)
			MediaConversionMethod.Remux -> stringResource(R.string.playback_info_direct_stream)
			MediaConversionMethod.Transcode -> stringResource(R.string.playback_info_transcoding)
		}
		InfoText(stringResource(R.string.playback_info_play_method, methodStr))

		// Container
		InfoText(stringResource(R.string.playback_info_container, stream.container.format.uppercase()))

		// Video info
		if (videoTrack != null) {
			InfoText("")
			InfoText(stringResource(R.string.playback_info_video_title))
			InfoText(stringResource(R.string.playback_info_codec, videoTrack.codec.uppercase()))
			if (videoTrack.width != null && videoTrack.height != null) {
				InfoText(stringResource(R.string.playback_info_resolution_str, "${videoTrack.width}x${videoTrack.height}"))
			}
			if (videoTrack.bitrate > 0) {
				InfoText(stringResource(R.string.playback_info_bitrate_str, formatBitrate(videoTrack.bitrate)))
			}
			videoTrack.videoRange?.let { InfoText(stringResource(R.string.playback_info_video_range, it)) }
		}

		// Audio info
		if (audioTrack != null) {
			InfoText("")
			InfoText(stringResource(R.string.playback_info_audio_title))
			InfoText(stringResource(R.string.playback_info_codec, audioTrack.codec.uppercase()))
			InfoText(stringResource(R.string.playback_info_channels, audioTrack.channels))
			if (audioTrack.bitrate > 0) {
				InfoText(stringResource(R.string.playback_info_bitrate_str, formatBitrate(audioTrack.bitrate)))
			}
		}

		// Transcoding note
		if (stream.conversionMethod == MediaConversionMethod.Transcode) {
			InfoText("")
			InfoText(stringResource(R.string.playback_info_transcoding_note))
		}
	}
}

@Composable
private fun formatBitrate(bitrate: Int): String {
	val mbitStr = stringResource(R.string.bitrate_mbit, 0f).substringAfter("0")
	val kbitStr = stringResource(R.string.bitrate_kbit, 0f).substringAfter("0")
	return when {
		bitrate >= 1_000_000 -> "%.1f$mbitStr".format(bitrate / 1_000_000f)
		else -> "%.0f$kbitStr".format(bitrate / 1_000f)
	}
}

@Composable
private fun InfoText(text: String) {
	Text(
		text = text,
		style = TextStyle(
			color = Color.White,
			fontSize = 14.sp,
		)
	)
}
