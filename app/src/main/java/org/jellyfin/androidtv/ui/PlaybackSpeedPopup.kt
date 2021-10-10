package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.Color
import org.jellyfin.androidtv.ui.ValueChangedListener
import android.widget.PopupWindow
import org.jellyfin.androidtv.ui.FloatSpinner
import android.view.Gravity
import android.view.LayoutInflater
import org.jellyfin.androidtv.R
import android.graphics.drawable.ColorDrawable
import android.view.View
import org.jellyfin.androidtv.util.Utils

class PlaybackSpeedPopup(context: Context?, anchor: View, listener: ValueChangedListener<Float>) {
    val popupWindow: PopupWindow?
    private val mAnchor: View
    private val mSpeedSpinner: FloatSpinner
    val isShowing: Boolean
        get() = popupWindow != null && popupWindow.isShowing

    fun show(value: Float) {
        mSpeedSpinner.value = value
        popupWindow!!.showAsDropDown(mAnchor, 0, 0, Gravity.END)
    }

    fun dismiss() {
        if (popupWindow != null && popupWindow.isShowing) {
            popupWindow.dismiss()
        }
    }

    init {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.playback_speed_popup, null)
        val width = Utils.convertDpToPixel(context!!, 240)
        val height = Utils.convertDpToPixel(context, 130)
        popupWindow = PopupWindow(layout, width, height)
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // necessary for popup to dismiss
        mAnchor = anchor
        mSpeedSpinner = layout.findViewById(R.id.floatSpinner)
        mSpeedSpinner.setOnChangeListener(listener)
    }
}
