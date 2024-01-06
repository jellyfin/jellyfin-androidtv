package org.jellyfin.androidtv.ui.shared

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.leanback.widget.TitleViewAdapter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.ViewLbTitleBinding


class TitleView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle), TitleViewAdapter.Provider
{
    private val binding: ViewLbTitleBinding = ViewLbTitleBinding.inflate(LayoutInflater.from(context), this)
    private val homeButton: ImageButton = binding.toolbarActions.findViewById(R.id.home)

    private val titleViewAdapter: TitleViewAdapter = object : TitleViewAdapter() {
        override fun setTitle(titleText: CharSequence) {
            this@TitleView.setTitle(titleText)
        }

        override fun getSearchAffordanceView(): View {
            return binding.titleOrb
        }
    }

    fun setTitle(title: CharSequence?) {
        if (title != null) {
            binding.titleText.text = title
            binding.titleText.visibility = VISIBLE
        }
    }

    override fun getTitleViewAdapter(): TitleViewAdapter {
        return titleViewAdapter
    }

    override fun onRequestFocusInDescendants(
        direction: Int,
        previouslyFocusedRect: Rect?
    ): Boolean {
        return homeButton.requestFocus() || super.onRequestFocusInDescendants(
            direction,
            previouslyFocusedRect
        )
    }
}