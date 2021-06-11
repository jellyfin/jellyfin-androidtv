package org.jellyfin.androidtv.ui.playback.nextup

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Space
import androidx.appcompat.widget.AppCompatButton
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

@KoinApiExtension
class NextUpButtons(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyle: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyle), KoinComponent {
	constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0, 0)

	private val mediaManager: MediaManager by inject()
	private var countdownTimer: CountDownTimer? = null
	private val view = View.inflate(context, R.layout.fragment_next_up_buttons, null)

	init {
		addView(view)
		view.findViewById<AppCompatButton>(R.id.fragment_next_up_buttons_play_next).apply {
			// Stop timer when unfocused
			setOnFocusChangeListener { _, focused -> if (!focused) stopTimer() }

			// Add initial focus
			requestFocus()
		}

		// Show skip button if episodes are shuffled
		if (mediaManager.isVideoQueueShuffled && mediaManager.currentVideoQueue.size > 1) {
			view.findViewById<Button>(R.id.fragment_next_up_buttons_skip).visibility = VISIBLE
			view.findViewById<Space>(R.id.fragment_next_up_buttons_skip_space).visibility = VISIBLE
		}
	}

	fun startTimer() {
		val duration = get<UserPreferences>()[UserPreferences.nextUpTimeout].toLong()

		// Cancel current timer if one is already set
		countdownTimer?.cancel()

		// Create timer
		countdownTimer = object : CountDownTimer(duration, 1) {
			override fun onTick(millisUntilFinished: Long) {
				view.findViewById<ProgressBar>(R.id.fragment_next_up_buttons_play_next_progress).apply {
					max = duration.toInt()
					progress = (duration - millisUntilFinished).toInt()
				}
			}

			override fun onFinish() {
				// Perform a click so the event handler will activate
				view.findViewById<AppCompatButton>(R.id.fragment_next_up_buttons_play_next).performClick()
			}
		}.start()
	}

	fun stopTimer() {
		countdownTimer?.cancel()

		// Hide progress bar
		view.findViewById<ProgressBar>(R.id.fragment_next_up_buttons_play_next_progress).apply {
			max = 0
			progress = 0
		}
	}

	fun setPlayNextListener(listener: (() -> Unit)?) {
		val button = view.findViewById<AppCompatButton>(R.id.fragment_next_up_buttons_play_next)

		if (listener == null) button.setOnClickListener(null)
		else button.setOnClickListener { listener() }
	}

	fun setSkipListener(listener: () -> Unit) {
		val button = view.findViewById<Button>(R.id.fragment_next_up_buttons_skip)

		if (listener == null) button.setOnClickListener(null)
		else button.setOnClickListener { listener() }
	}

	fun setCancelListener(listener: () -> Unit) {
		val button = view.findViewById<Button>(R.id.fragment_next_up_buttons_cancel)

		if (listener == null) button.setOnClickListener(null)
		else button.setOnClickListener { listener() }
	}
}
