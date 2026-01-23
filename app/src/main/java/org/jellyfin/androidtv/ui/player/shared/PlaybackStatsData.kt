package org.jellyfin.androidtv.ui.player.shared

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlayMethod

/**
 * Shared data model for playback stats overlay.
 * Used by the legacy View-based player.
 */
data class PlaybackStatsData(
	val itemName: String?,
	val playState: String,
	val positionSeconds: Long,
	val durationSeconds: Long,
	val speed: Float,
	val playbackMethod: String?,
	val container: String?,
	val videoCodec: String?,
	val videoResolution: String?,
	val videoHdr: String?,
	val videoColorSpace: String?,
	val videoColorPrimaries: String?,
	val audioCodec: String?,
	val audioChannels: Int?,
	val audioSampleRate: Int?,
	val audioBitrate: Int?,
) {
	companion object {
		/**
		 * Extract stats from legacy player's MediaSourceInfo
		 */
		fun fromMediaSourceInfo(
			item: BaseItemDto?,
			mediaSource: MediaSourceInfo?,
			playState: String,
			positionSeconds: Long,
			durationSeconds: Long,
			speed: Float,
			playMethod: PlayMethod? = null,
			videoWidth: Int? = null,
			videoHeight: Int? = null,
		): PlaybackStatsData {
			val videoStream = mediaSource?.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }
			val audioStream = mediaSource?.mediaStreams?.firstOrNull { it.type == MediaStreamType.AUDIO }

			// Prefer actual video dimensions from player, fallback to stream metadata
			val resolution = if (videoWidth != null && videoHeight != null && videoWidth > 0 && videoHeight > 0) {
				"${videoWidth}x${videoHeight}"
			} else if (videoStream?.width != null && videoStream.height != null) {
				"${videoStream.width}x${videoStream.height}"
			} else {
				null
			}

			// Format playback method
			val playbackMethodStr = when (playMethod) {
				PlayMethod.DIRECT_PLAY -> "Direct Play"
				PlayMethod.DIRECT_STREAM -> "Direct Stream"
				PlayMethod.TRANSCODE -> "Transcode"
				else -> null
			}

			return PlaybackStatsData(
				itemName = item?.name,
				playState = playState,
				positionSeconds = positionSeconds,
				durationSeconds = durationSeconds,
				speed = speed,
				playbackMethod = playbackMethodStr,
				container = mediaSource?.container,
				videoCodec = videoStream?.codec,
				videoResolution = resolution,
				videoHdr = videoStream?.videoRangeType?.serialName?.takeIf {
					it.isNotBlank() && it.uppercase() != "SDR" && it.uppercase() != "UNKNOWN"
				}?.uppercase(),
				videoColorSpace = videoStream?.colorSpace?.takeIf { it.isNotBlank() },
				videoColorPrimaries = videoStream?.colorPrimaries?.takeIf { it.isNotBlank() },
				audioCodec = audioStream?.codec,
				audioChannels = audioStream?.channels?.takeIf { it > 0 },
				audioSampleRate = audioStream?.sampleRate?.takeIf { it > 0 },
				audioBitrate = audioStream?.bitRate?.takeIf { it > 0 },
			)
		}
	}
}
