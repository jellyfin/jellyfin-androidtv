package org.jellyfin.androidtv.ui

import android.content.Context
import android.view.Gravity
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.res.ResourcesCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.Utils

/**
 * Button used in [JumpList].
 */
class TextButton(
	context: Context,
	text: String?,
	size: Float,
	listener: OnClickListener?
) : AppCompatButton(context) {
	init {
		// Set box size
		layoutParams = Utils.convertDpToPixel(context, size).let { pixels ->
			ViewGroup.LayoutParams(pixels * 2 + 15, pixels + 40)
		}

		setBackgroundColor(0)
		textSize = size
		gravity = Gravity.CENTER
		setText(text)

		onFocusChangeListener = OnFocusChangeListener { view, hasFocus ->
			val color = if (hasFocus) ResourcesCompat.getColor(resources, R.color.lb_default_brand_color, null) else 0
			view.setBackgroundColor(color)
		}

		setOnClickListener(listener)
	}
}
