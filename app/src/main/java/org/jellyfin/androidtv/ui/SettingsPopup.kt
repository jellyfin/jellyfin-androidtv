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
	anchor: View,
	audioChangedListener: ValueChangedListener<Long>?,
	subtitleChangedListener: ValueChangedListener<Long>?
) {
	val popupWindow: PopupWindow?
	private val anchor: View
	private val binding: SettingsPopupBinding

	init {
		val width = Utils.convertDpToPixel(context, width)
		val height = Utils.convertDpToPixel(context, fullHeight)

		binding = SettingsPopupBinding.inflate(LayoutInflater.from(context), null, false)

		popupWindow = PopupWindow(binding.root, width, height)
		popupWindow.isFocusable = true
		popupWindow.isOutsideTouchable = true
		popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // necessary for popup to dismiss

		this.anchor = anchor

		binding.subtitleDelay.increment = subtitleDelayIncrement
		binding.subtitleDelay.valueChangedListener = subtitleChangedListener

		binding.audioDelay.valueChangedListener = audioChangedListener

		binding.subtitleDelayTxt
	}

	private fun setSubtitlesPresent(context: Context, subtitlesPresent: Boolean) {
		if(!subtitlesPresent)
		{
			binding.subtitleDelayTxt.visibility = View.GONE
			binding.subtitleDelay.visibility = View.GONE
			popupWindow?.height = Utils.convertDpToPixel(context, singleHeight)
		}
		else
		{
			binding.subtitleDelayTxt.visibility = View.VISIBLE
			binding.subtitleDelay.visibility = View.VISIBLE
			popupWindow?.height = Utils.convertDpToPixel(context, fullHeight)
		}
	}

	fun show(context: Context, subtitlesPresent: Boolean, audioDelayVal: Long, subtitleDelayVal: Long) {
		setSubtitlesPresent(context, subtitlesPresent)

		binding.audioDelay.value = audioDelayVal
		binding.subtitleDelay.value = subtitleDelayVal
		popupWindow!!.showAsDropDown(anchor, 0, 0, Gravity.END)
	}

	companion object {
		private const val width = 240
		private const val fullHeight = 230
		private const val singleHeight = 130
		private const val subtitleDelayIncrement = 500L
	}
}
