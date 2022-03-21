package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.HorizontalScrollView

class ObservableHorizontalScrollView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = android.R.attr.horizontalScrollViewStyle,
	defStyleRes: Int = 0,
) : HorizontalScrollView(context, attrs, defStyleAttr, defStyleRes) {
	var scrollViewListener: HorizontalScrollViewListener? = null

	override fun onScrollChanged(x: Int, y: Int, oldx: Int, oldy: Int) {
		super.onScrollChanged(x, y, oldx, oldy)
		scrollViewListener?.onScrollChanged(this, x, y, oldx, oldy)
	}
}
