package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * A view that limits its height to the window
 */
class FullHeightView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
	private val measureRect = Rect()

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val maximumHeight = measureRect.also { getWindowVisibleDisplayFrame(it) }.height()
		val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maximumHeight, MeasureSpec.AT_MOST)
		super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
	}
}
