package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.PopupExpandableTextViewBinding

class ExpandableTextView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {
	private val popupContentBinding = PopupExpandableTextViewBinding.inflate(
		LayoutInflater.from(context),
		null,
		false
	)
	private val popup = PopupWindow(
		popupContentBinding.root,
		ViewGroup.LayoutParams.MATCH_PARENT,
		ViewGroup.LayoutParams.MATCH_PARENT,
		true
	).apply {
		animationStyle = R.style.WindowAnimation_Fade
	}

	init {
		background = ContextCompat.getDrawable(context, R.drawable.expanded_text)

		popupContentBinding.scrollContainer.setOnClickListener { popup.dismiss() }

		setOnClickListener {
			// Update text
			popupContentBinding.content.text = this@ExpandableTextView.text

			// Show popup
			popup.showAtLocation(rootView, Gravity.CENTER, 0, 0)
			popup.isOutsideTouchable = true

			// Focus on scroll view
			popupContentBinding.scrollContainer.requestFocus()
		}
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)

		// Only make focusable when text is ellipsized.
		val isEllipsized = !text.isNullOrBlank() && lineCount > 0 && layout.getEllipsisCount(lineCount - 1) > 0
		isFocusable = isEllipsized
		isClickable = isEllipsized
	}
}
