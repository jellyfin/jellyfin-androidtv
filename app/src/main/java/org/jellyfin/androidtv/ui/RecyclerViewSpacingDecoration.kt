package org.jellyfin.androidtv.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewSpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
	override fun getItemOffsets(rect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) = with(rect) {
		var isVertical = true

		// Apply LinearLayoutManager orientation
		val layoutManager = parent.layoutManager
		if (layoutManager is LinearLayoutManager)
			isVertical = layoutManager.orientation == LinearLayoutManager.VERTICAL

		// Only apply spacing between items
		if (parent.getChildAdapterPosition(view) != 0) {
			if (isVertical) top = spacing
			else left = spacing
		}
	}
}
