package org.jellyfin.androidtv.ui.shared

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.ViewToolbarBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.koin.java.KoinJavaComponent.get

class ToolbarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	private val binding = ViewToolbarBinding.inflate(LayoutInflater.from(context), this, true)

	init {
		val clockBehavior = get(UserPreferences::class.java)[UserPreferences.clockBehavior]
		binding.toolbarClock.isVisible = clockBehavior != ClockBehavior.NEVER && clockBehavior != ClockBehavior.IN_VIDEO

		val style = context.theme.obtainStyledAttributes(attrs, R.styleable.JellyfinTheme, defStyleAttr, defStyleRes)
		binding.toolbarRoot.background = style.getDrawable(R.styleable.JellyfinTheme_toolbarBackground)
	}

	// Add child views to the content slot
	override fun addView(child: View) =
		if (child.id == R.id.toolbar_root) super.addView(child)
		else binding.toolbarContent.addView(child)

	override fun addView(child: View, params: ViewGroup.LayoutParams) =
		if (child.id == R.id.toolbar_root) super.addView(child, params)
		else binding.toolbarContent.addView(child, params)

	override fun addView(child: View, index: Int) =
		if (child.id == R.id.toolbar_root) super.addView(child, index)
		else binding.toolbarContent.addView(child, index)

	override fun addView(child: View, width: Int, height: Int) =
		if (child.id == R.id.toolbar_root) super.addView(child, width, height)
		else binding.toolbarContent.addView(child, width, height)

	override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) =
		if (child.id == R.id.toolbar_root) super.addView(child, index, params)
		else binding.toolbarContent.addView(child, index, params)

	override fun removeView(child: View) =
		if (child.id == R.id.toolbar_root) super.removeView(child)
		else binding.toolbarContent.removeView(child)
}
