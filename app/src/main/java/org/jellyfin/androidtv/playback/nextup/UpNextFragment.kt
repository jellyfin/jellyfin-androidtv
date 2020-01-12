package org.jellyfin.androidtv.playback.nextup

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.leanback.app.BackgroundManager
import kotlinx.android.synthetic.main.fragment_upnext_row.*
import kotlinx.android.synthetic.main.fragment_upnext_row.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.playback.PlaybackOverlayActivity
import org.jellyfin.androidtv.util.toHtmlSpanned

class UpNextFragment(private val data: UpNextItemData) : Fragment() {
	private var countdownTimer: CountDownTimer? = null

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_upnext_row, container, false).apply {
			image.setOnClickListener {
				stopCountdown()
				playNext()
			}

			BackgroundManager.getInstance(activity).setBitmap(data.backdrop)

			image.setImageBitmap(data.thumbnail)
			title.text = data.title
			description.text = data.description?.toHtmlSpanned()
		}
	}

	override fun onResume() {
		super.onResume()

		startCountdown()
	}

	override fun onPause() {
		super.onPause()

		stopCountdown()
	}

	private fun startCountdown() {
		val duration = 30 * 1000L // 5 seconds

		// Cancel current timer if one is already set
		countdownTimer?.cancel()

		// Set progress bar visible
		upnext_countdown.visibility = View.VISIBLE

		// Create timer
		countdownTimer = object : CountDownTimer(duration, 1) {
			override fun onTick(millisUntilFinished: Long) {
				upnext_countdown.max = duration.toInt()
				upnext_countdown.progress = millisUntilFinished.toInt()
			}

			override fun onFinish() {
				playNext()
			}
		}.start()
	}

	fun stopCountdown() {
		countdownTimer?.cancel()
		countdownTimer = null

		upnext_countdown.visibility = View.GONE
	}

	fun isCountdownActive() = countdownTimer != null

	private fun playNext() {
		stopCountdown()

		val intent = Intent(activity, PlaybackOverlayActivity::class.java)
		startActivity(intent)
		activity?.finish()
	}
}
