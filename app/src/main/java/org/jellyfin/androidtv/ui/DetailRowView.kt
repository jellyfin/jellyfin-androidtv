package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.jellyfin.androidtv.databinding.ViewRowDetailsBinding

class DetailRowView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	val binding = ViewRowDetailsBinding.inflate(LayoutInflater.from(context), this, true)

	private var lastFocusedButton: TextUnderButton? = null

	init {
		// Keep track of the last selected button and reselect it when navigating back to the buttons row
		binding.root.viewTreeObserver.addOnGlobalFocusChangeListener { oldFocus, newFocus ->
			if (oldFocus is TextUnderButton && newFocus !is TextUnderButton) {
				lastFocusedButton = oldFocus
			} else if (newFocus is TextUnderButton && lastFocusedButton != null) {
				lastFocusedButton?.requestFocus()
				lastFocusedButton = null
			}
		}
	}
}
