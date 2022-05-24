package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import org.jellyfin.androidtv.databinding.SettingsPopupBinding
import org.jellyfin.androidtv.util.Utils

class SettingsPopup(
	context: Context?,
	anchor: View,
	audioChangedListener: ValueChangedListener<Long>?,
	subtitleChangedListener: ValueChangedListener<Long>?
) {
	val popupWindow: PopupWindow?
	private val mAnchor: View
	private val mAudioDelaySpinner: NumberSpinnerView
	private val mSubtitleDelaySpinner: NumberSpinnerView
	private val mAudioDelaySpinnerTxt: TextView

	val isShowing: Boolean
		get() = popupWindow != null && popupWindow.isShowing

	fun show(audioDelayVal: Long, subtitleDelayVal: Long) {
		mAudioDelaySpinner.value = audioDelayVal
		mSubtitleDelaySpinner.value = subtitleDelayVal
		popupWindow!!.showAsDropDown(mAnchor, 0, 0, Gravity.END)
	}

	fun setSubtitlesPresent(context: Context, subtitlesPresent: Boolean) {
		if(!subtitlesPresent)
		{
			mSubtitleDelaySpinner.visibility = View.GONE
			mSubtitleDelaySpinner.visibility = View.GONE
			popupWindow?.height = Utils.convertDpToPixel(context,130)
		}
		else
		{
			mSubtitleDelaySpinner.visibility = View.VISIBLE
			mSubtitleDelaySpinner.visibility = View.VISIBLE
			popupWindow?.height = Utils.convertDpToPixel(context,230)
		}
	}

	init {
		val inflater = LayoutInflater.from(context)
		val binding = SettingsPopupBinding.inflate(inflater, null, false)
		val width = Utils.convertDpToPixel(context!!, 240)
		val height = Utils.convertDpToPixel(context, 230)

		popupWindow = PopupWindow(binding.root, width, height)
		popupWindow.isFocusable = true
		popupWindow.isOutsideTouchable = true
		popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // necessary for popup to dismiss

		mAnchor = anchor

		mSubtitleDelaySpinner = binding.subtitleDelay
		mSubtitleDelaySpinner.increment = 500L
		mSubtitleDelaySpinner.valueChangedListener = subtitleChangedListener

		mAudioDelaySpinner = binding.audioDelay
		mAudioDelaySpinner.valueChangedListener = audioChangedListener

		mAudioDelaySpinnerTxt = binding.subtitleDelayTxt
	}
}
