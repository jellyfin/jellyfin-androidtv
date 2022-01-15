package org.jellyfin.androidtv.ui.shared

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R

class StrokeTextView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : AppCompatTextView(context, attrs) {
	var strokeWidth = 0.0f
	private var isDrawing: Boolean = false

	init {
		val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.StrokeTextView)
		strokeWidth = styledAttrs.getFloat(R.styleable.StrokeTextView_stroke_width, 0.0f)
	}

	override fun invalidate() {
		// To prevent infinite call of onDraw because setTextColor calls invalidate()
		if (isDrawing) return
		super.invalidate()
	}

	override fun onDraw(canvas: Canvas?) {
		if (strokeWidth <= 0) {
			return super.onDraw(canvas)
		}
		isDrawing = true
		val initialColor = textColors

		paint.style = Paint.Style.STROKE
		paint.strokeWidth = strokeWidth
		setTextColor(ContextCompat.getColor(context, R.color.black))
		super.onDraw(canvas)

		paint.style = Paint.Style.FILL
		setTextColor(initialColor)
		super.onDraw(canvas)
		isDrawing = false
	}
}
