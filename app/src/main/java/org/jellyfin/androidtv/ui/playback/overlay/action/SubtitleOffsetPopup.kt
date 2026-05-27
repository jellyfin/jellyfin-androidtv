package org.jellyfin.androidtv.ui.playback.overlay.action

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.DialogSubtitleOffsetBinding
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import java.util.Locale
import kotlin.math.abs

class SubtitleOffsetPopup(
	private val context: Context,
) {
	companion object {
		private const val OFFSET_US_100MS = 100_000L
		private const val OFFSET_US_500MS = 500_000L
		private const val KEY_REPEAT_INITIAL_DELAY_MS = 350L
		private const val KEY_REPEAT_INTERVAL_MS = 80L
	}

	private val repeatHandler = Handler(Looper.getMainLooper())
	private var repeatDeltaUs = 0L
	private val repeatRunnable = object : Runnable {
		override fun run() {
			val activeDialog = dialog ?: return
			if (!activeDialog.isShowing || repeatDeltaUs == 0L) return
			repeatAction?.invoke(repeatDeltaUs)
			repeatHandler.postDelayed(this, KEY_REPEAT_INTERVAL_MS)
		}
	}
	private var repeatAction: ((Long) -> Unit)? = null

	private var dialog: Dialog? = null
	val isShowing: Boolean get() = dialog?.isShowing == true

	fun show(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
	) {
		if (isShowing) return

		videoPlayerAdapter.leanbackOverlayFragment.enterSubtitleOffsetMode()

		val binding = DialogSubtitleOffsetBinding.inflate(
			android.view.LayoutInflater.from(context)
		)
		fun updateOffsetText() {
			binding.subtitleOffsetCurrent.text = context.getString(
				R.string.lbl_subtitle_offset_current,
				context.getString(
					R.string.lbl_subtitle_offset_seconds,
					formatSubtitleOffsetSeconds(playbackController.subtitleTimingOffsetUs),
				),
			)
		}
		fun applyOffsetDelta(deltaUs: Long) {
			playbackController.adjustSubtitleTimingOffsetUs(deltaUs)
			updateOffsetText()
		}
		fun resetOffset() {
			playbackController.resetSubtitleTimingOffset()
			updateOffsetText()
		}
		updateOffsetText()

		binding.btnOffsetMinus500.text = context.getString(R.string.lbl_subtitle_offset_seconds, formatSubtitleOffsetSeconds(-OFFSET_US_500MS))
		binding.btnOffsetMinus100.text = context.getString(R.string.lbl_subtitle_offset_seconds, formatSubtitleOffsetSeconds(-OFFSET_US_100MS))
		binding.btnOffsetPlus100.text  = context.getString(R.string.lbl_subtitle_offset_seconds, formatSubtitleOffsetSeconds(OFFSET_US_100MS))
		binding.btnOffsetPlus500.text  = context.getString(R.string.lbl_subtitle_offset_seconds, formatSubtitleOffsetSeconds(OFFSET_US_500MS))

		binding.btnOffsetMinus500.setOnClickListener { applyOffsetDelta(-OFFSET_US_500MS) }
		binding.btnOffsetMinus100.setOnClickListener { applyOffsetDelta(-OFFSET_US_100MS) }
		binding.btnOffsetPlus100.setOnClickListener  { applyOffsetDelta(OFFSET_US_100MS) }
		binding.btnOffsetPlus500.setOnClickListener  { applyOffsetDelta(OFFSET_US_500MS) }
		binding.btnReset.setOnClickListener          { resetOffset() }
		binding.btnHelp.setOnClickListener {
			binding.subtitleOffsetHelpText.visibility =
				if (binding.subtitleOffsetHelpText.visibility == View.VISIBLE) View.GONE
				else View.VISIBLE
		}

		repeatAction = { deltaUs -> applyOffsetDelta(deltaUs) }

		val panelTopMargin = context.resources.getDimensionPixelSize(R.dimen.subtitle_offset_panel_top_margin)

		dialog = Dialog(context, R.style.Theme_Jellyfin_Dialog).apply {
			requestWindowFeature(Window.FEATURE_NO_TITLE)
			setContentView(binding.root)
			setCanceledOnTouchOutside(false)
			window?.apply {
				setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
				clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
				setDimAmount(0f)
				attributes = attributes.apply {
					width = WindowManager.LayoutParams.WRAP_CONTENT
					height = WindowManager.LayoutParams.WRAP_CONTENT
					gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
					y = panelTopMargin
				}
			}
			setOnKeyListener { _, keyCode, event ->
				when (keyCode) {
					KeyEvent.KEYCODE_BACK -> {
						if (event.action == KeyEvent.ACTION_UP && binding.subtitleOffsetHelpText.visibility == View.VISIBLE) {
							binding.subtitleOffsetHelpText.visibility = View.GONE
							true
						} else {
							false
						}
					}
					KeyEvent.KEYCODE_DPAD_UP -> handleRepeatKey(event, OFFSET_US_100MS)
					KeyEvent.KEYCODE_DPAD_DOWN -> handleRepeatKey(event, -OFFSET_US_100MS)
					else -> false
				}
			}
			setOnDismissListener {
				stopRepeatingAdjustments()
				repeatAction = null
				videoPlayerAdapter.leanbackOverlayFragment.exitSubtitleOffsetMode()
				dialog = null
			}
			show()
			binding.btnOffsetMinus500.requestFocus()
		}
	}

	fun dismiss() {
		dialog?.dismiss()
	}

	private fun handleRepeatKey(event: KeyEvent, deltaUs: Long): Boolean {
		return when (event.action) {
			KeyEvent.ACTION_DOWN -> {
				if (event.repeatCount == 0) {
					repeatAction?.invoke(deltaUs)
					startRepeatingAdjustments(deltaUs)
				}
				true
			}
			KeyEvent.ACTION_UP -> {
				stopRepeatingAdjustments()
				true
			}
			else -> false
		}
	}

	private fun startRepeatingAdjustments(deltaUs: Long) {
		repeatDeltaUs = deltaUs
		repeatHandler.removeCallbacks(repeatRunnable)
		repeatHandler.postDelayed(repeatRunnable, KEY_REPEAT_INITIAL_DELAY_MS)
	}

	private fun stopRepeatingAdjustments() {
		repeatDeltaUs = 0L
		repeatHandler.removeCallbacks(repeatRunnable)
	}

}
