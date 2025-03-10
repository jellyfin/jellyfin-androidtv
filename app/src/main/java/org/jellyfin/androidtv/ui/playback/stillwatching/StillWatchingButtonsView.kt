package org.jellyfin.androidtv.ui.playback.stillwatching

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.jellyfin.androidtv.databinding.FragmentStillWatchingButtonsBinding

class StillWatchingButtonsView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
	private var countdownTimer: CountDownTimer? = null
	private val view = FragmentStillWatchingButtonsBinding.inflate(LayoutInflater.from(context), this, true)

	var countdownTimerEnabled: Boolean = false
	var duration: Int = 0

	init {
		view.fragmentStillWatchingButtonsYes.apply {
			// Stop timer when unfocused
			setOnFocusChangeListener { _, focused -> if (!focused) stopTimer() }
		}
	}

	fun focusNoButton() = view.fragmentStillWatchingButtonsNo.requestFocus()

	fun startTimer() {
		// Cancel current timer if one is already set
		countdownTimer?.cancel()

		if (!countdownTimerEnabled) return

		// Create timer
		countdownTimer = object : CountDownTimer(duration.toLong(), 1) {
			override fun onTick(millisUntilFinished: Long) {
				view.fragmentStillWatchingButtonsNoProgress.apply {
					max = duration
					progress = (duration - millisUntilFinished).toInt()
				}
			}

			override fun onFinish() {
				// Perform a click so the event handler will activate
				view.fragmentStillWatchingButtonsNoProgress.performClick()
			}
		}.start()
	}

	fun stopTimer() {
		countdownTimer?.cancel()

		// Hide progress bar
		view.fragmentStillWatchingButtonsNoProgress.apply {
			max = 0
			progress = 0
		}
	}

	fun setYesListener(listener: () -> Unit) {
		view.fragmentStillWatchingButtonsYes.setOnClickListener { listener() }
	}

	fun setNoListener(listener: () -> Unit) {
		view.fragmentStillWatchingButtonsNo.setOnClickListener { listener() }
	}
}

