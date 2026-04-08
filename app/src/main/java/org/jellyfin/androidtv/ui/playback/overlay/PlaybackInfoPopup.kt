package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlayMethod

/**
 * Data class containing playback information for display.
 */
data class PlaybackInfoData(
	val playMethod: PlayMethod?,
	val container: String?,
	val videoCodec: String?,
	val videoResolution: String?,
	val videoBitrate: String?,
	val videoRange: String?,
	val audioCodec: String?,
	val audioChannels: Int?,
	val audioBitrate: String?,
	val audioLanguage: String?,
	val isTranscoding: Boolean,
) {
	companion object {
		fun from(
			playMethod: PlayMethod?,
			container: String?,
			mediaSource: MediaSourceInfo?,
			formatBitrate: (Long) -> String,
		): PlaybackInfoData {
			val videoStream = mediaSource?.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }
			val audioStream = mediaSource?.mediaStreams?.firstOrNull { it.type == MediaStreamType.AUDIO }

			return PlaybackInfoData(
				playMethod = playMethod,
				container = container?.uppercase(),
				videoCodec = videoStream?.codec?.uppercase(),
				videoResolution = if (videoStream?.width != null && videoStream.height != null) {
					"${videoStream.width}x${videoStream.height}"
				} else null,
				videoBitrate = videoStream?.bitRate?.let { formatBitrate(it.toLong()) },
				videoRange = videoStream?.videoRangeType?.name,
				audioCodec = audioStream?.codec?.uppercase(),
				audioChannels = audioStream?.channels,
				audioBitrate = audioStream?.bitRate?.let { formatBitrate(it.toLong()) },
				audioLanguage = audioStream?.language,
				isTranscoding = playMethod == PlayMethod.TRANSCODE,
			)
		}
	}
}

class PlaybackInfoPopup(
	private val context: Context,
	private val playbackController: PlaybackController,
) {
	private var popupWindow: PopupWindow? = null
	private var onDismissListener: (() -> Unit)? = null

	fun setOnDismissListener(listener: () -> Unit) {
		onDismissListener = listener
	}

	fun show(anchor: View) {
		val data = PlaybackInfoData.from(
			playMethod = playbackController.currentStreamInfo?.playMethod,
			container = playbackController.currentStreamInfo?.container,
			mediaSource = playbackController.currentMediaSource,
			formatBitrate = { bitrate ->
				when {
					bitrate >= 1_000_000 -> context.getString(R.string.bitrate_mbit, bitrate / 1_000_000f)
					else -> context.getString(R.string.bitrate_kbit, bitrate / 1_000f)
				}
			}
		)

		val textView = TextView(context).apply {
			text = buildInfoText(data)
			setTextColor(ContextCompat.getColor(context, R.color.white))
			textSize = 14f
			setPadding(32, 24, 32, 24)
			setBackgroundColor(ContextCompat.getColor(context, R.color.black_transparent))
		}

		popupWindow = PopupWindow(
			textView,
			android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
			android.view.ViewGroup.LayoutParams.WRAP_CONTENT
		).apply {
			isOutsideTouchable = true
			isFocusable = true
			setOnDismissListener { onDismissListener?.invoke() }
			showAtLocation(anchor, Gravity.TOP or Gravity.START, 48, 48)
		}
	}

	fun dismiss() {
		popupWindow?.dismiss()
		popupWindow = null
	}

	private fun buildInfoText(data: PlaybackInfoData): String = buildString {
		val methodStr = when (data.playMethod) {
			PlayMethod.DIRECT_PLAY -> context.getString(R.string.playback_info_direct_play)
			PlayMethod.DIRECT_STREAM -> context.getString(R.string.playback_info_direct_stream)
			else -> context.getString(R.string.playback_info_transcoding)
		}
		appendLine(context.getString(R.string.playback_info_play_method, methodStr))

		data.container?.let { appendLine(context.getString(R.string.playback_info_container, it)) }

		if (data.videoCodec != null) {
			appendLine()
			appendLine(context.getString(R.string.playback_info_video_title))
			appendLine(context.getString(R.string.playback_info_codec, data.videoCodec))
			data.videoResolution?.let { appendLine(context.getString(R.string.playback_info_resolution_str, it)) }
			data.videoBitrate?.let { appendLine(context.getString(R.string.playback_info_bitrate_str, it)) }
			data.videoRange?.let { appendLine(context.getString(R.string.playback_info_video_range, it)) }
		}

		if (data.audioCodec != null) {
			appendLine()
			appendLine(context.getString(R.string.playback_info_audio_title))
			appendLine(context.getString(R.string.playback_info_codec, data.audioCodec))
			data.audioChannels?.let { appendLine(context.getString(R.string.playback_info_channels, it)) }
			data.audioBitrate?.let { appendLine(context.getString(R.string.playback_info_bitrate_str, it)) }
			data.audioLanguage?.let { appendLine(context.getString(R.string.playback_info_language, it)) }
		}

		if (data.isTranscoding) {
			appendLine()
			appendLine(context.getString(R.string.playback_info_transcoding_note))
		}
	}
}

// Compose version for new player
@Composable
fun PlaybackInfoContent(
	data: PlaybackInfoData,
	modifier: Modifier = Modifier,
) {
	Column(
		verticalArrangement = Arrangement.spacedBy(4.dp),
		modifier = modifier
			.background(Color.Black.copy(alpha = 0.8f))
			.padding(16.dp)
	) {
		val methodStr = when (data.playMethod) {
			PlayMethod.DIRECT_PLAY -> stringResource(R.string.playback_info_direct_play)
			PlayMethod.DIRECT_STREAM -> stringResource(R.string.playback_info_direct_stream)
			else -> stringResource(R.string.playback_info_transcoding)
		}
		InfoText(stringResource(R.string.playback_info_play_method, methodStr))

		data.container?.let { InfoText(stringResource(R.string.playback_info_container, it)) }

		if (data.videoCodec != null) {
			InfoText("")
			InfoText(stringResource(R.string.playback_info_video_title))
			InfoText(stringResource(R.string.playback_info_codec, data.videoCodec))
			data.videoResolution?.let { InfoText(stringResource(R.string.playback_info_resolution_str, it)) }
			data.videoBitrate?.let { InfoText(stringResource(R.string.playback_info_bitrate_str, it)) }
			data.videoRange?.let { InfoText(stringResource(R.string.playback_info_video_range, it)) }
		}

		if (data.audioCodec != null) {
			InfoText("")
			InfoText(stringResource(R.string.playback_info_audio_title))
			InfoText(stringResource(R.string.playback_info_codec, data.audioCodec))
			data.audioChannels?.let { InfoText(stringResource(R.string.playback_info_channels, it)) }
			data.audioBitrate?.let { InfoText(stringResource(R.string.playback_info_bitrate_str, it)) }
			data.audioLanguage?.let { InfoText(stringResource(R.string.playback_info_language, it)) }
		}

		if (data.isTranscoding) {
			InfoText("")
			InfoText(stringResource(R.string.playback_info_transcoding_note))
		}
	}
}

@Composable
private fun InfoText(text: String) {
	Text(
		text = text,
		style = androidx.compose.ui.text.TextStyle(
			color = Color.White,
			fontSize = 14.sp,
		)
	)
}
