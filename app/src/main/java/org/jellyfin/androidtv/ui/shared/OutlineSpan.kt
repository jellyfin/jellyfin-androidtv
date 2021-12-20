package org.jellyfin.androidtv.ui.shared

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.ReplacementSpan

/**
 * A class that draws the outlines of a text
 */
class OutlineSpan(
): ReplacementSpan() {

	override fun getSize(
		paint: Paint,
		text: CharSequence,
		start: Int,
		end: Int,
		fm: Paint.FontMetricsInt?
	): Int {
		if (fm != null) {
			fm.ascent = paint.fontMetricsInt.ascent
			fm.bottom = paint.fontMetricsInt.bottom
			fm.descent = paint.fontMetricsInt.descent
			fm.leading = paint.fontMetricsInt.leading
			fm.top = paint.fontMetricsInt.top
		}

		return paint.measureText(text, start, end).toInt()
	}

	override fun draw(
		canvas: Canvas,
		text: CharSequence,
		start: Int,
		end: Int,
		x: Float,
		top: Int,
		y: Int,
		bottom: Int,
		paint: Paint
	) {
		val initialColor = paint.color

		paint.color = Color.BLACK
		paint.style = Paint.Style.STROKE
		paint.strokeWidth = 4f
		canvas.drawText(text, start, end, x, y.toFloat(), paint)

		paint.color = initialColor
		paint.style = Paint.Style.FILL
		canvas.drawText(text, start, end, x, y.toFloat(), paint)
	}
}
