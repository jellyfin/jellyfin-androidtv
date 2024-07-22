package org.jellyfin.androidtv.ui.card

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.text.bold
import org.jellyfin.androidtv.databinding.ViewCardMediaInfoBinding
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import java.text.NumberFormat

class MediaInfoCardView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	private val binding = ViewCardMediaInfoBinding.inflate(LayoutInflater.from(getContext()), this, true)

	fun setMediaStream(mediaStream: MediaStream) {
		binding.title.text = mediaStream.type.toString()
		binding.entries.text = SpannableStringBuilder().apply {
			if (mediaStream.type != MediaStreamType.VIDEO) mediaStream.language?.let { addRow("Language", it) }
			mediaStream.codec?.let { addRow("Codec", it) }
			mediaStream.profile?.let { addRow("Profile", it) }
			mediaStream.level?.let { addRow("Level", it.toString()) }
			mediaStream.channelLayout?.let { addRow("Layout", it) }

			if (mediaStream.type == MediaStreamType.VIDEO) {
				if (mediaStream.width != null && mediaStream.height != null) addRow("Resolution", "${mediaStream.width}x${mediaStream.height}")
				if (mediaStream.isAnamorphic == true) addRow("Anamorphic")
				if (mediaStream.isInterlaced) addRow("Interlaced")
				mediaStream.aspectRatio?.let { addRow("Aspect", it) }
				mediaStream.realFrameRate?.let { addRow("Framerate", it.toString()) }
			}

			if (mediaStream.type == MediaStreamType.AUDIO) {
				mediaStream.channels?.let { addRow("Channels", it.toString()) }
				mediaStream.sampleRate?.let { addRow("Sample rate", it.toString()) }
				mediaStream.bitDepth?.let { addRow("Bit depth", it.toString()) }
			}

			mediaStream.bitRate?.let { addRow("Bitrate", NumberFormat.getInstance().format((it / 1024).toLong()) + " kbps") }
			if (mediaStream.isDefault) addRow("Default")
			if (mediaStream.isForced) addRow("Forced")
			if (mediaStream.isExternal) addRow("External")
		}
	}

	private fun SpannableStringBuilder.addRow(
		label: String,
		value: String? = null,
	) {
		bold { append(label) }

		if (value != null) {
			append(": ")
			append(value)
		}

		appendLine()
	}
}
