package org.jellyfin.androidtv.ui.shared

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class StrokeTextView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr), KoinComponent {
	var strokeWidth = 0.0f
	private var isDrawing: Boolean = false
	private val subtitlesTextColor = get<UserPreferences>()[UserPreferences.subtitlesTextColor]

	init {
		val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.StrokeTextView)
		strokeWidth = styledAttrs.getFloat(R.styleable.StrokeTextView_strokeWidth, 0.0f)
		styledAttrs.recycle()
	}

	override fun invalidate() {
		// To prevent infinite call of onDraw because setTextColor calls invalidate()
		if (isDrawing) return
		super.invalidate()
	}

	override fun onDraw(canvas: Canvas) {
		if (strokeWidth <= 0) return super.onDraw(canvas)
		isDrawing = true
		paint.isAntiAlias = true
		paint.strokeWidth = strokeWidth
		paint.style = Paint.Style.STROKE
		paint.strokeJoin = Paint.Join.ROUND
		setTextColor(ContextCompat.getColor(context, R.color.black))
		super.onDraw(canvas)

		paint.style = Paint.Style.FILL
		setTextColor(ColorStateList.valueOf(subtitlesTextColor.toInt()))
		super.onDraw(canvas)
		isDrawing = false
	}
}
