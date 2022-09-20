package org.jellyfin.androidtv.ui.shared

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.style.LineBackgroundSpan
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import kotlin.math.roundToInt

/**
 * A LineBackgroundSpan that supports horizontal leading/trailing padding.
 * Requires the TextView to set a shadow layer and padding to work as expected.
 */
class PaddedLineBackgroundSpan(
	@ColorInt private val backgroundColor: Int,
	@Dimension private val horizontalPadding: Int,
	private val horizontalGravity: Int,
) : LineBackgroundSpan {
	private val backgroundRect = Rect()

	override fun drawBackground(
		canvas: Canvas,
		paint: Paint,
		left: Int,
		right: Int,
		top: Int,
		baseline: Int,
		bottom: Int,
		text: CharSequence,
		start: Int,
		end: Int,
		lineNumber: Int
	) {
		val initialColor = paint.color
		// Measure the current line of text
		val textWidth = paint.measureText(text, start, end).roundToInt()

		@Suppress("RtlHardcoded")
		val rectLeft = when(horizontalGravity) {
			Gravity.CENTER_HORIZONTAL -> ((right - textWidth) / 2) - horizontalPadding
			Gravity.RIGHT -> right - textWidth - horizontalPadding
			else -> left - horizontalPadding
		}

		@Suppress("RtlHardcoded")
		val rectRight = when(horizontalGravity) {
			Gravity.CENTER_HORIZONTAL -> right - rectLeft
			Gravity.RIGHT -> right + horizontalPadding
			else -> left + textWidth + horizontalPadding
		}

		// Set the dimensions of the background rectangle
		backgroundRect.set(
			rectLeft,
			top,
			rectRight,
			bottom
		)

		// Draw the background rectangle
		paint.color = backgroundColor
		canvas.drawRect(backgroundRect, paint)

		// Reset the paint color to the initial value
		paint.color = initialColor
	}
}
