package org.jellyfin.androidtv.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewSpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
	//todo Only works for horizontal recyclerviews
	override fun getItemOffsets(rect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) = with(rect) {
		if (parent.getChildAdapterPosition(view) != 0) {
			left = spacing
		}
	}
}
