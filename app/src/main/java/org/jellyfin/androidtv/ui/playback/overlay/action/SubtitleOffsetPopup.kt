package org.jellyfin.androidtv.ui.playback.overlay.action

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.jellyfin.androidtv.R
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

		val density = context.resources.displayMetrics.density
		val panelPaddingHorizontal = (18 * density).toInt()
		val panelPaddingVertical = (14 * density).toInt()
		val rowSpacing = (10 * density).toInt()
		val buttonSpacing = (8 * density).toInt()
		val panelTopMargin = (32 * density).toInt()

		val container = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			background = context.getDrawable(R.drawable.subtitle_offset_panel)
			setPadding(panelPaddingHorizontal, panelPaddingVertical, panelPaddingHorizontal, panelPaddingVertical)
		}

		val currentOffsetText = TextView(context).apply {
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
			text = context.getString(
				R.string.lbl_subtitle_offset_current,
				formatOffset(playbackController.subtitleTimingOffsetUs),
			)
		}
		fun updateOffsetText() {
			currentOffsetText.text = context.getString(
				R.string.lbl_subtitle_offset_current,
				formatOffset(playbackController.subtitleTimingOffsetUs),
			)
		}
		fun applyOffsetDelta(offsetUs: Long) {
			playbackController.adjustSubtitleTimingOffsetUs(offsetUs)
			updateOffsetText()
		}
		fun resetOffset() {
			playbackController.resetSubtitleTimingOffset()
			updateOffsetText()
		}
		container.addView(
			currentOffsetText,
			LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			),
		)

		val adjustmentRow = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_HORIZONTAL
		}

		fun createButton(label: String, onClick: () -> Unit): Button = Button(context).apply {
			text = label
			isAllCaps = false
			background = context.getDrawable(R.drawable.subtitle_offset_button)
			setTextColor(Color.WHITE)
			stateListAnimator = null
			minimumWidth = 0
			minWidth = 0
			minHeight = 0
			minimumHeight = 0
			setPadding((14 * density).toInt(), (8 * density).toInt(), (14 * density).toInt(), (8 * density).toInt())
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
			setOnClickListener {
				onClick()
				updateOffsetText()
			}
		}

		val adjustmentButtons = listOf(
			createButton("-0.5s") { applyOffsetDelta(-OFFSET_US_500MS) },
			createButton("-0.1s") { applyOffsetDelta(-OFFSET_US_100MS) },
			createButton("+0.1s") { applyOffsetDelta(OFFSET_US_100MS) },
			createButton("+0.5s") { applyOffsetDelta(OFFSET_US_500MS) },
			createButton(context.getString(R.string.lbl_reset)) { resetOffset() },
		)
		adjustmentButtons.forEachIndexed { index, button ->
			adjustmentRow.addView(
				button,
				LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT,
				).apply {
					if (index > 0) marginStart = buttonSpacing
				},
			)
		}
		container.addView(
			adjustmentRow,
			LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
			).apply {
				this.topMargin = rowSpacing
				gravity = Gravity.CENTER_HORIZONTAL
			},
		)

		repeatAction = { deltaUs -> applyOffsetDelta(deltaUs) }

		dialog = Dialog(context, R.style.Theme_Jellyfin_Dialog).apply {
			requestWindowFeature(Window.FEATURE_NO_TITLE)
			setContentView(container)
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
			adjustmentButtons.firstOrNull()?.requestFocus()
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

	private fun formatOffset(offsetUs: Long): String {
		val seconds = offsetUs / 1_000_000.0
		val safeSeconds = if (abs(seconds) < 0.05) 0.0 else seconds
		return String.format(Locale.US, "%+.1fs", safeSeconds)
	}
}
