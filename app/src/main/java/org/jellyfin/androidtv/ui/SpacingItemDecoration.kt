package org.jellyfin.androidtv.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * [RecyclerView.ItemDecoration] that adds spacing between elements.
 * It does this by dividing the spacing in half and applying it as offset to all individual views.
 * This causes the first and last item to be offset to their parent view by 50%.
 */
class SpacingItemDecoration(
	private val horizontalSpacing: Int,
	private val verticalSpacing: Int,
) : RecyclerView.ItemDecoration() {
	override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
		val horizontalPx = (horizontalSpacing * parent.context.resources.displayMetrics.density / 2).roundToInt()
		val verticalPx = (verticalSpacing * parent.context.resources.displayMetrics.density / 2).roundToInt()

		outRect.set(horizontalPx, verticalPx, horizontalPx, verticalPx)
	}
}
