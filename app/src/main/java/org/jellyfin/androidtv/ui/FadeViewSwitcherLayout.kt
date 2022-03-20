package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import androidx.annotation.IntRange
import org.jellyfin.androidtv.integration.dream.LibraryDreamService

class FadeViewSwitcherLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	companion object {
		const val VIEW_NONE = -1
	}

	private var currentView = VIEW_NONE
	private val nextViewId
		get() = if (currentView >= childCount - 1) 0 else currentView + 1
	private val previousViewId
		get() = if (currentView <= 0) childCount - 1 else currentView - 1

	override fun onViewAdded(child: View?) {
		super.onViewAdded(child)

		// Hide new views by default
		child?.alpha = 0f
	}

	fun showNextView() = showView(nextViewId)
	fun showPreviousView() = showView(previousViewId)
	fun hideAllViews() = showView(VIEW_NONE)

	fun <V : View> getNextView() = getChildAt(nextViewId) as V
	fun <V : View> getCurrentView() = if (currentView == VIEW_NONE) null else currentView as V
	fun <V : View> getPreviousView() = getChildAt(previousViewId) as V

	fun showView(@IntRange(from = -1) view: Int) {
		// TODO: right now crossfades, need to be smarter to hide old view after new view is completely visible
		if (currentView != VIEW_NONE) getChildAt(currentView).fadeOut()
		if (view != VIEW_NONE) getChildAt(view).fadeIn()

		currentView = view
	}

	private fun View.fadeIn() {
		alpha = 1f
		startAnimation(AlphaAnimation(0f, 1f).apply {
			duration = LibraryDreamService.TRANSITION_DURATION
			fillAfter = true
		})
	}

	private fun View.fadeOut() = startAnimation(AlphaAnimation(1f, 0f).apply {
		duration = LibraryDreamService.TRANSITION_DURATION
		fillAfter = true
	})
}
