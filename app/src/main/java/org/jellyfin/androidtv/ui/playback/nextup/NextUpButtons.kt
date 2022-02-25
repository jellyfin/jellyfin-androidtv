package org.jellyfin.androidtv.ui.playback.nextup

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.jellyfin.androidtv.databinding.FragmentNextUpButtonsBinding

class NextUpButtons(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyle: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyle) {
	constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0, 0)

	private var countdownTimer: CountDownTimer? = null
	private val view = FragmentNextUpButtonsBinding.inflate(LayoutInflater.from(context), this, true)

	var countdownTimerEnabled: Boolean = false
	var duration: Int = 0

	init {
		view.fragmentNextUpButtonsPlayNext.apply {
			// Stop timer when unfocused
			setOnFocusChangeListener { _, focused -> if (!focused) stopTimer() }

			// Add initial focus
			requestFocus()
		}
	}

	fun startTimer() {
		// Cancel current timer if one is already set
		countdownTimer?.cancel()

		if (!countdownTimerEnabled) return

		// Create timer
		countdownTimer = object : CountDownTimer(duration.toLong(), 1) {
			override fun onTick(millisUntilFinished: Long) {
				view.fragmentNextUpButtonsPlayNextProgress.apply {
					max = duration
					progress = (duration - millisUntilFinished).toInt()
				}
			}

			override fun onFinish() {
				// Perform a click so the event handler will activate
				view.fragmentNextUpButtonsPlayNext.performClick()
			}
		}.start()
	}

	fun stopTimer() {
		countdownTimer?.cancel()

		// Hide progress bar
		view.fragmentNextUpButtonsPlayNextProgress.apply {
			max = 0
			progress = 0
		}
	}

	fun setPlayNextListener(listener: () -> Unit) {
		view.fragmentNextUpButtonsPlayNext.setOnClickListener { listener() }
	}

	fun setCancelListener(listener: () -> Unit) {
		view.fragmentNextUpButtonsCancel.setOnClickListener { listener() }
	}
}
