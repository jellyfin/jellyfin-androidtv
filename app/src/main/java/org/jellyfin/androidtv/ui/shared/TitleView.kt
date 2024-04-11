package org.jellyfin.androidtv.ui.shared

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.leanback.widget.TitleViewAdapter
import org.jellyfin.androidtv.databinding.ViewLbTitleBinding

class TitleView @JvmOverloads constructor(
	context: Context?,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), TitleViewAdapter.Provider {
	private val binding = ViewLbTitleBinding.inflate(LayoutInflater.from(context), this)

	private val titleViewAdapter: TitleViewAdapter = object : TitleViewAdapter() {
		override fun setTitle(titleText: CharSequence) {
			binding.titleText.text = title
			binding.titleText.isVisible = true
		}

		override fun getSearchAffordanceView(): View = binding.titleOrb
	}

	override fun getTitleViewAdapter() = titleViewAdapter

	override fun onRequestFocusInDescendants(
		direction: Int,
		previouslyFocusedRect: Rect?
	): Boolean {
		if (binding.toolbarActions.homeButton.requestFocus()) return true

		return super.onRequestFocusInDescendants(direction, previouslyFocusedRect)
	}
}
