package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.MediaStreamAudioTrack
import org.jellyfin.playback.core.mediastream.MediaStreamTrack
import org.jellyfin.playback.core.mediastream.MediaStreamVideoTrack
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.koin.compose.koinInject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun VideoPlayerStatsOverlay(
	playbackManager: PlaybackManager = koinInject(),
	modifier: Modifier = Modifier,
) {
	val entry by rememberQueueEntry(playbackManager)
	val playState by playbackManager.state.playState.collectAsState()
	val speed by playbackManager.state.speed.collectAsState()
	val videoSize by playbackManager.state.videoSize.collectAsState()
	val positionInfo = playbackManager.state.positionInfo

	val item = entry?.baseItem
	val mediaStream = entry?.mediaStream

	Box(
		modifier = modifier
			.background(Color.Black.copy(alpha = 0.8f))
			.padding(12.dp)
			.fillMaxWidth(0.4f)
			.fillMaxHeight(0.6f)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			// Title
			Text(
				text = item?.name ?: "Stats for Nerds",
				fontSize = 16.sp,
				fontWeight = FontWeight.Bold,
				color = Color.White,
				modifier = Modifier.padding(bottom = 4.dp)
			)

			// Basic playback state
			StatsRow("State", playState.name)
			val positionSeconds = positionInfo.active.toInt(DurationUnit.SECONDS)
			val durationSeconds = positionInfo.duration.toInt(DurationUnit.SECONDS)
			StatsRow("Position", "${positionSeconds}s / ${durationSeconds}s")
			StatsRow("Speed", String.format("%.2fx", speed))

			// Playback method
			val playbackMethod = when (mediaStream?.conversionMethod) {
				MediaConversionMethod.None -> "Direct Play"
				MediaConversionMethod.Remux -> "Direct Stream"
				MediaConversionMethod.Transcode -> "Transcode"
				null -> null
			}
			playbackMethod?.let { method -> StatsRow("Playback", method) }

			// Stream info
			mediaStream?.container?.format?.let { StatsRow("Container", it) }

			// Video section
			val videoTrack = mediaStream?.tracks?.firstOrNull { track -> track is MediaStreamVideoTrack } as? MediaStreamVideoTrack
			var hasVideo = false
			videoTrack?.let { track ->
				if (!hasVideo) {
					StatsSectionHeader("Video")
					hasVideo = true
				}
				StatsRow("Codec", track.codec)
			}
			if (videoSize.width > 0 && videoSize.height > 0) {
				if (!hasVideo) {
					StatsSectionHeader("Video")
					hasVideo = true
				}
				StatsRow("Resolution", "${videoSize.width}x${videoSize.height}")
			}
			videoTrack?.videoRangeType?.let {
				if (!hasVideo) {
					StatsSectionHeader("Video")
					hasVideo = true
				}
				StatsRow("HDR", it)
			}
			videoTrack?.colorSpace?.let {
				if (!hasVideo) {
					StatsSectionHeader("Video")
					hasVideo = true
				}
				StatsRow("Color space", it)
			}
			videoTrack?.colorPrimaries?.let {
				if (!hasVideo) {
					StatsSectionHeader("Video")
					hasVideo = true
				}
				StatsRow("Color primaries", it)
			}

			// Audio section
			val audioTrack = mediaStream?.tracks?.firstOrNull { track -> track is MediaStreamAudioTrack } as? MediaStreamAudioTrack
			var hasAudio = false
			audioTrack?.let { track ->
				if (!hasAudio) {
					StatsSectionHeader("Audio")
					hasAudio = true
				}
				StatsRow("Codec", track.codec)
				if (track.channels > 0) {
					StatsRow("Channels", track.channels.toString())
				}
				if (track.sampleRate > 0) {
					StatsRow("Sample rate", "${track.sampleRate} Hz")
				}
				if (track.bitrate > 0) {
					val kbps = track.bitrate / 1000f
					StatsRow("Bitrate", String.format("%.0f kbps", kbps))
				}
			}
		}
	}
}

@Composable
private fun StatsSectionHeader(text: String) {
	Text(
		text = text,
		fontSize = 14.sp,
		fontWeight = FontWeight.Bold,
		color = Color.White,
		modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
	)
}

@Composable
private fun StatsRow(label: String, value: String) {
	Text(
		text = "$label: $value",
		fontSize = 12.sp,
		color = Color.White,
	)
}
