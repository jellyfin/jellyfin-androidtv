package org.jellyfin.androidtv.playback.nextup

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.fragment_next_up_buttons.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.preferences.UserPreferences

class NextUpButtons(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyle: Int = 0) : FrameLayout(context, attrs, defStyleAttr, defStyle) {
	constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0, 0)

	private var countdownTimer: CountDownTimer? = null
	private val view = View.inflate(context, R.layout.fragment_next_up_buttons, null)

	init {
		addView(view)
		view.fragment_next_up_buttons_play_next.apply {
			// Stop timer when unfocused
			setOnFocusChangeListener { _, focused -> if (!focused) stopTimer() }

			// Add initial focus
			requestFocus()
		}
	}

	fun startTimer() {
		val duration = TvApp.getApplication().userPreferences[UserPreferences.nextUpTimeout].toLong()

		// Cancel current timer if one is already set
		countdownTimer?.cancel()

		// Create timer
		countdownTimer = object : CountDownTimer(duration, 1) {
			override fun onTick(millisUntilFinished: Long) {
				fragment_next_up_buttons_play_next_progress.max = duration.toInt()
				fragment_next_up_buttons_play_next_progress.progress = (duration - millisUntilFinished).toInt()
			}

			override fun onFinish() {
				// Perform a click so the event handler will activate
				view.fragment_next_up_buttons_play_next.performClick()
			}
		}.start()
	}

	fun stopTimer() {
		countdownTimer?.cancel()

		// Hide progress bar
		fragment_next_up_buttons_play_next_progress.max = 0
		fragment_next_up_buttons_play_next_progress.progress = 0
	}

	fun setPlayNextListener(listener: (() -> Unit)?) {
		val button = view.fragment_next_up_buttons_play_next

		if (listener == null) button.setOnClickListener(null)
		else button.setOnClickListener { listener() }
	}

	fun setCancelListener(listener: () -> Unit) {
		val button = view.fragment_next_up_buttons_cancel

		if (listener == null) button.setOnClickListener(null)
		else button.setOnClickListener { listener() }
	}
}
