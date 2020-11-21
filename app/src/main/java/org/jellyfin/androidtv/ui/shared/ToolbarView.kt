package org.jellyfin.androidtv.ui.shared

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_toolbar.view.*
import org.jellyfin.androidtv.R

class ToolbarView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
	init {
		inflate(context, R.layout.view_toolbar, this)
	}

	// Add child views to the content slot
	override fun addView(child: View) =
		if (child.id == R.id.toolbar_root) super.addView(child)
		else toolbar_content.addView(child)

	override fun addView(child: View, params: ViewGroup.LayoutParams) =
		if (child.id == R.id.toolbar_root) super.addView(child, params)
		else toolbar_content.addView(child, params)

	override fun addView(child: View, index: Int) =
		if (child.id == R.id.toolbar_root) super.addView(child, index)
		else toolbar_content.addView(child, index)

	override fun addView(child: View, width: Int, height: Int) =
		if (child.id == R.id.toolbar_root) super.addView(child, width, height)
		else toolbar_content.addView(child, width, height)

	override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) =
		if (child.id == R.id.toolbar_root) super.addView(child, index, params)
		else toolbar_content.addView(child, index, params)

	override fun removeView(child: View) =
		if (child.id == R.id.toolbar_root) super.removeView(child)
		else toolbar_content.removeView(child)
}
