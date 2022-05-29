package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import androidx.core.view.isVisible
import org.jellyfin.androidtv.databinding.SettingsPopupBinding
import org.jellyfin.androidtv.util.dp

class SettingsPopup(
	private val context: Context,
	private val anchor: View,
	audioChangedListener: ValueChangedListener<Long>?,
	subtitleChangedListener: ValueChangedListener<Long>?
) {
	private val binding = SettingsPopupBinding.inflate(LayoutInflater.from(context), null, false)
		.apply {
			subtitleDelay.increment = SUBTITLE_DELAY_INCREMENT
			subtitleDelay.valueChangedListener = subtitleChangedListener
			audioDelay.valueChangedListener = audioChangedListener
		}

	val popupWindow = PopupWindow(binding.root, WIDTH.dp(context), FULL_HEIGHT.dp(context))
		.apply {
			isFocusable = true
			isOutsideTouchable = true
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
		}

	fun show(
		hasSubtitles: Boolean,
		audioDelay: Long,
		subtitleDelay: Long
	) {
		binding.audioDelay.value = audioDelay

		binding.subtitleDelayTxt.isVisible = hasSubtitles
		binding.subtitleDelay.isVisible = hasSubtitles
		binding.subtitleDelay.value = subtitleDelay

		popupWindow.height = (if (hasSubtitles) FULL_HEIGHT else SINGLE_HEIGHT).dp(context)
		popupWindow.showAsDropDown(anchor, 0, 0, Gravity.END)
	}

	companion object {
		private const val WIDTH = 240
		private const val FULL_HEIGHT = 230
		private const val SINGLE_HEIGHT = 130
		private const val SUBTITLE_DELAY_INCREMENT = 500L
	}
}
