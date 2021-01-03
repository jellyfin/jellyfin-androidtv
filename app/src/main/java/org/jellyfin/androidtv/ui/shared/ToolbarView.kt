package org.jellyfin.androidtv.ui.shared

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.koin.java.KoinJavaComponent.get

class ToolbarView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
	init {
		inflate(context, R.layout.view_toolbar, this)
		val clockBehavior = get(UserPreferences::class.java)[UserPreferences.clockBehavior]
		if (clockBehavior == ClockBehavior.NEVER || clockBehavior == ClockBehavior.IN_VIDEO)
			findViewById<TextClock>(R.id.toolbar_clock).visibility = GONE
	}

	// Add child views to the content slot
	override fun addView(child: View) =
		if (child.id == R.id.toolbar_root) super.addView(child)
		else findViewById<LinearLayout>(R.id.toolbar_content).addView(child)

	override fun addView(child: View, params: ViewGroup.LayoutParams) =
		if (child.id == R.id.toolbar_root) super.addView(child, params)
		else findViewById<LinearLayout>(R.id.toolbar_content).addView(child, params)

	override fun addView(child: View, index: Int) =
		if (child.id == R.id.toolbar_root) super.addView(child, index)
		else findViewById<LinearLayout>(R.id.toolbar_content).addView(child, index)

	override fun addView(child: View, width: Int, height: Int) =
		if (child.id == R.id.toolbar_root) super.addView(child, width, height)
		else findViewById<LinearLayout>(R.id.toolbar_content).addView(child, width, height)

	override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) =
		if (child.id == R.id.toolbar_root) super.addView(child, index, params)
		else findViewById<LinearLayout>(R.id.toolbar_content).addView(child, index, params)

	override fun removeView(child: View) =
		if (child.id == R.id.toolbar_root) super.removeView(child)
		else findViewById<LinearLayout>(R.id.toolbar_content).removeView(child)
}
