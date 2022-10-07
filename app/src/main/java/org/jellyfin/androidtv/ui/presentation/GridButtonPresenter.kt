package org.jellyfin.androidtv.ui.presentation

import android.util.TypedValue
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.card.LegacyImageCardView

class GridButtonPresenter @JvmOverloads constructor(
	private val showinfo: Boolean = true,
	private val width: Int = 220,
	private val height: Int = 220,
) : Presenter() {
	class ViewHolder(
		private val cardView: LegacyImageCardView,
	) : Presenter.ViewHolder(cardView) {
		var item: GridButton? = null
			set(value) {
				field = value

				if (value != null) cardView.mainImageView.setImageResource(value.imageRes)

				cardView.setTitleText(value?.text)
				cardView.setOverlayText(value?.text)
			}
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val typedValue = TypedValue()
		parent.context.theme.resolveAttribute(R.attr.cardViewBackground, typedValue, true)

		val view = LegacyImageCardView(parent.context, showinfo).apply {
			isFocusable = true
			isFocusableInTouchMode = true
			setBackgroundColor(typedValue.data)
		}
		view.setMainImageDimensions(width, height)

		return ViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: Presenter.ViewHolder?, item: Any?) {
		if (item !is GridButton) return
		if (viewHolder !is ViewHolder) return

		viewHolder.item = item
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder?) = Unit
	override fun onViewAttachedToWindow(viewHolder: Presenter.ViewHolder?) = Unit
}
