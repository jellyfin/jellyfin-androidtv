package org.jellyfin.androidtv.util

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.VideoManager
import java.util.logging.Logger

interface PromptUserCallback {
	fun onResult(result: Boolean)
}

object WatchTracker {
	private var episodeCount = 0
	private var watchTime = 0L
	private var lastUpdateTime= 0L
	private var lastInteractionTime = System.currentTimeMillis()
	private val watchTimeHandler = Handler(Looper.getMainLooper())
	private var isPlaying: Boolean = false

	private class WatchTimeUpdater(
		private val context: Context,
		private val videoManager: VideoManager
	) : Runnable {
		override fun run() {
			if (isPlaying) {
				val currentTime = System.currentTimeMillis()
				watchTime += currentTime - lastUpdateTime
				lastUpdateTime = currentTime
				checkPrompt(context, videoManager)
				watchTimeHandler.postDelayed(this, 1000)
			}
		}
	}

	fun onEpisodeWatched() {
		Logger.getLogger(WatchTracker::class.java.name).info("Watcher onEpisodeWatched")
		episodeCount++
	}

	fun onUserInteraction() {
		Logger.getLogger(WatchTracker::class.java.name).info("Watcher onUserInteraction")
		resetWatchTime()
		lastInteractionTime = System.currentTimeMillis()
	}

	private fun checkPrompt(context: Context, videoManager: VideoManager) {
		if (episodeCount == 3 || watchTime >= 90 * 60 * 1000) {
			videoManager.pause()
			promptUser(context, object : PromptUserCallback {
				override fun onResult(result: Boolean) {
					if (result) {
						videoManager.play()
					} else {
						context.getActivity()?.finish()
					}
				}
			})
		}
	}

	private fun promptUser(context: Context, callback: PromptUserCallback) {
		val dialog = AlertDialog.Builder(context)
			.setTitle(context.getString(R.string.still_watching_title))
			.setMessage(context.getString(R.string.continue_watching_message))
			.setPositiveButton(R.string.lbl_yes) { dialog, _ ->
				dialog.dismiss()
				callback.onResult(true)
			}
			.setNegativeButton(R.string.lbl_no) { dialog, _ ->
				dialog.dismiss()
				callback.onResult(false)
			}
			.setCancelable(false)
			.create()

		dialog.show()

		val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
		val handler = Handler(Looper.getMainLooper())
		val startTime = System.currentTimeMillis()
		val countdownTime = 15000L // 15 seconds

		handler.post(object : Runnable {
			override fun run() {
				val elapsedTime = System.currentTimeMillis() - startTime
				val remainingTime = (countdownTime - elapsedTime) / 1000
				if (remainingTime > 0) {
					negativeButton.text = context.getString(R.string.no_button_text_with_time, remainingTime)
					handler.postDelayed(this, 1000)
				} else {
					if (dialog.isShowing) {
						dialog.dismiss()
						callback.onResult(false)
					}
				}
			}
		})
	}

	fun startWatchTime(context: Context, videoManager: VideoManager) {
		Logger.getLogger(WatchTracker::class.java.name).info("Start tracker")
		resetWatchTime()
		lastUpdateTime = System.currentTimeMillis()
		watchTimeHandler.post(WatchTimeUpdater(context, videoManager))
	}

	fun stopWatchTime() {
		isPlaying = false
		watchTimeHandler.removeCallbacksAndMessages(null)
	}

	private fun resetWatchTime() {
		watchTime = 0L
		episodeCount = 0
		isPlaying = true
	}
}
