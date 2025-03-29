package org.jellyfin.androidtv.ui.playback.stillwatching

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.jellyfin.androidtv.databinding.FragmentStillWatchingButtonsBinding

class StillWatchingButtonsView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
	private val view = FragmentStillWatchingButtonsBinding.inflate(LayoutInflater.from(context), this, true)

	var duration: Int = 0

	fun focusYesButton() = view.fragmentStillWatchingButtonsYes.requestFocus()

	fun setYesListener(listener: () -> Unit) {
		view.fragmentStillWatchingButtonsYes.setOnClickListener { listener() }
	}

	fun setNoListener(listener: () -> Unit) {
		view.fragmentStillWatchingButtonsNo.setOnClickListener { listener() }
	}
}

