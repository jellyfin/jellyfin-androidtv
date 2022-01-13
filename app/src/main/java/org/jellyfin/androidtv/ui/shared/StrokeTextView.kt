package org.jellyfin.androidtv.ui.shared

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R

class StrokeTextView : AppCompatTextView {
	private var strokeWidth = 0.0f
	private var isDrawing: Boolean = false

	constructor(
		context: Context,
		attrs: AttributeSet?,
		defStyle: Int
	) : super(context, attrs, defStyle) {
		obtainStyledAttrs(context, attrs)
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		obtainStyledAttrs(context, attrs)
	}

	constructor(context: Context) : super(context) {
		obtainStyledAttrs(context, attrs = null)
	}

	private fun obtainStyledAttrs(context: Context, attrs: AttributeSet?) {
		val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.StrokeTextView)
		strokeWidth = styledAttrs.getFloat(
			R.styleable.StrokeTextView_stroke_width, 0.0f
		)
		styledAttrs.recycle()
	}

	// To prevent infinite call of onDraw because setTextColor calls invalidate()
	override fun invalidate() {
		if (isDrawing) return
		super.invalidate()
	}

	fun setStrokeWidth(strokeWidth: Float){
		this.strokeWidth = strokeWidth
	}

	override fun onDraw(canvas: Canvas?) {
		if (strokeWidth > 0) {
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
		} else {
			super.onDraw(canvas)
		}
	}
}
