package org.jellyfin.androidtv.ui.card

import android.content.Context
import android.widget.FrameLayout
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.text.bold
import androidx.core.text.toSpannable
import org.jellyfin.androidtv.databinding.ViewCardMediaInfoBinding
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.apiclient.model.entities.MediaStream
import org.jellyfin.apiclient.model.entities.MediaStreamType
import java.text.NumberFormat
import java.util.*

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
			if (mediaStream.type != MediaStreamType.Video) mediaStream.language?.let { addRow("Language", it) }
			mediaStream.codec?.let { addRow("Codec", it) }
			mediaStream.profile?.let { addRow("Profile", it) }
			mediaStream.level?.let { addRow("Level", it.toString()) }
			mediaStream.channelLayout?.let { addRow("Layout", it) }

			if (mediaStream.type == MediaStreamType.Video) {
				if (mediaStream.width != null && mediaStream.height != null) addRow("Resolution", "${mediaStream.width}x${mediaStream.height}")
				if (mediaStream.isAnamorphic == true) addRow("Anamorphic")
				if (mediaStream.isInterlaced) addRow("Interlaced")
				mediaStream.aspectRatio?.let { addRow("Aspect", it) }
				mediaStream.realFrameRate?.let { addRow("Framerate", it.toString()) }
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
