package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.FrameLayout
import org.jellyfin.androidtv.R

/**
 * A view that limits its height to the window
 */
class FullHeightView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
	private val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.FullHeightView, 0, 0)
	private val measureRect = Rect()
	private val forced = attributes.getBoolean(R.styleable.FullHeightView_forced, false)

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val maximumHeight = measureRect.also { getWindowVisibleDisplayFrame(it) }.height()
		val mode = if (forced) MeasureSpec.EXACTLY else MeasureSpec.AT_MOST

		if (maximumHeight > 0) {
			val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maximumHeight, mode)
			super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		}
	}
}
