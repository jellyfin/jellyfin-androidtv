package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import org.jellyfin.androidtv.databinding.SubtitleDelayPopupBinding
import org.jellyfin.androidtv.util.Utils

class SubtitleDelayPopup(context: Context?, anchor: View, listener: ValueChangedListener<Long>?) {
	val popupWindow: PopupWindow?
	private val mAnchor: View
	private val mDelaySpinner: NumberSpinnerView
	val isShowing: Boolean
		get() = popupWindow != null && popupWindow.isShowing

	fun show(value: Long) {
		mDelaySpinner.value = value
		popupWindow!!.showAsDropDown(mAnchor, 0, 0, Gravity.END)
	}

	init {
		val inflater = LayoutInflater.from(context)
		val binding = SubtitleDelayPopupBinding.inflate(inflater, null, false)
		val width = Utils.convertDpToPixel(context!!, 240)
		val height = Utils.convertDpToPixel(context, 130)

		popupWindow = PopupWindow(binding.root, width, height)
		popupWindow.isFocusable = true
		popupWindow.isOutsideTouchable = true
		popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // necessary for popup to dismiss

		mAnchor = anchor

		mDelaySpinner = binding.subtitleDelay
		mDelaySpinner.increment = 500L
		mDelaySpinner.valueChangedListener = listener
	}
}
