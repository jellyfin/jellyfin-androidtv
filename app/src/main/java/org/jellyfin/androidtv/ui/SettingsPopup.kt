package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import org.jellyfin.androidtv.databinding.SettingsPopupBinding
import org.jellyfin.androidtv.util.Utils

class SettingsPopup(
	context: Context,
	private val anchor: View,
	audioChangedListener: ValueChangedListener<Long>?,
	subtitleChangedListener: ValueChangedListener<Long>?
) {
	private val binding: SettingsPopupBinding =
		SettingsPopupBinding.inflate(LayoutInflater.from(context), null, false)
			.apply {
				subtitleDelay.increment = SUBTITLE_DELAY_INCREMENT
				subtitleDelay.valueChangedListener = subtitleChangedListener
				audioDelay.valueChangedListener = audioChangedListener
			}

	private val width = Utils.convertDpToPixel(context, WIDTH)
	private val height = Utils.convertDpToPixel(context, FULL_HEIGHT)
	val popupWindow: PopupWindow = PopupWindow(binding.root, width, height)
		.apply {
			isFocusable = true
			isOutsideTouchable = true
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
		}

	private fun setSubtitlesPresent(context: Context, subtitlesPresent: Boolean) {
		if (!subtitlesPresent) {
			binding.subtitleDelayTxt.visibility = View.GONE
			binding.subtitleDelay.visibility = View.GONE
			popupWindow.height = Utils.convertDpToPixel(context, SINGLE_HEIGHT)
		} else {
			binding.subtitleDelayTxt.visibility = View.VISIBLE
			binding.subtitleDelay.visibility = View.VISIBLE
			popupWindow.height = Utils.convertDpToPixel(context, FULL_HEIGHT)
		}
	}

	fun show(
		context: Context,
		subtitlesPresent: Boolean,
		audioDelayVal: Long,
		subtitleDelayVal: Long
	) {
		setSubtitlesPresent(context, subtitlesPresent)

		binding.audioDelay.value = audioDelayVal
		binding.subtitleDelay.value = subtitleDelayVal
		popupWindow.showAsDropDown(anchor, 0, 0, Gravity.END)
	}

	companion object {
		private const val WIDTH = 240
		private const val FULL_HEIGHT = 230
		private const val SINGLE_HEIGHT = 130
		private const val SUBTITLE_DELAY_INCREMENT = 500L
	}
}
