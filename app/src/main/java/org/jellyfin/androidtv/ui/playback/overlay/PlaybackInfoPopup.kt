package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlayMethod

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
		val textView = TextView(context).apply {
			text = buildInfoText()
			setTextColor(ContextCompat.getColor(context, R.color.white))
			textSize = 14f
			setPadding(32, 24, 32, 24)
			setBackgroundColor(ContextCompat.getColor(context, R.color.black_transparent))
		}

		popupWindow = PopupWindow(textView, 
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

	private fun buildInfoText(): String = buildString {
		val streamInfo = playbackController.currentStreamInfo
		val mediaSource = playbackController.currentMediaSource
		val item = playbackController.currentlyPlayingItem

		// Play method
		val playMethod = streamInfo?.playMethod ?: PlayMethod.TRANSCODE
		val methodStr = when (playMethod) {
			PlayMethod.DIRECT_PLAY -> context.getString(R.string.playback_info_direct_play)
			PlayMethod.DIRECT_STREAM -> context.getString(R.string.playback_info_direct_stream)
			PlayMethod.TRANSCODE -> context.getString(R.string.playback_info_transcoding)
		}
		appendLine(context.getString(R.string.playback_info_play_method, methodStr))

		// Container
		streamInfo?.container?.let { container ->
			appendLine(context.getString(R.string.playback_info_container, container.uppercase()))
		}

		// Video stream info
		mediaSource?.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }?.let { video ->
			appendLine()
			appendLine(context.getString(R.string.playback_info_video_title))
			
			video.codec?.let { codec ->
				appendLine(context.getString(R.string.playback_info_codec, codec.uppercase()))
			}
			
			if (video.width != null && video.height != null) {
				appendLine(context.getString(R.string.playback_info_resolution, video.width, video.height))
			}
			
			video.bitRate?.let { bitrate ->
				appendLine(context.getString(R.string.playback_info_bitrate, formatBitrate(bitrate.toLong())))
			}

			video.videoRangeType.let { rangeType ->
				appendLine(context.getString(R.string.playback_info_video_range, rangeType.name))
			}
		}

		// Audio stream info
		val audioIndex = playbackController.audioStreamIndex
		val audioStream = mediaSource?.mediaStreams
			?.filter { it.type == MediaStreamType.AUDIO }
			?.let { streams ->
				if (audioIndex >= 0) streams.getOrNull(audioIndex) else streams.firstOrNull()
			}
		
		audioStream?.let { audio ->
			appendLine()
			appendLine(context.getString(R.string.playback_info_audio_title))
			
			audio.codec?.let { codec ->
				appendLine(context.getString(R.string.playback_info_codec, codec.uppercase()))
			}
			
			audio.channels?.let { channels ->
				appendLine(context.getString(R.string.playback_info_channels, channels))
			}
			
			audio.bitRate?.let { bitrate ->
				appendLine(context.getString(R.string.playback_info_bitrate, formatBitrate(bitrate.toLong())))
			}
			
			audio.language?.let { lang ->
				appendLine(context.getString(R.string.playback_info_language, lang))
			}
		}

		// Transcoding reason (if applicable)
		if (playMethod == PlayMethod.TRANSCODE) {
			mediaSource?.transcodingUrl?.let {
				appendLine()
				appendLine(context.getString(R.string.playback_info_transcoding_note))
			}
		}
	}

	private fun formatBitrate(bitrate: Long): String {
		return when {
			bitrate >= 1_000_000 -> context.getString(R.string.bitrate_mbit, bitrate / 1_000_000f)
			else -> context.getString(R.string.bitrate_kbit, bitrate / 1_000f)
		}
	}
}
