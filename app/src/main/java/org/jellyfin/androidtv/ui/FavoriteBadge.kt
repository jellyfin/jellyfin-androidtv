package org.jellyfin.androidtv.ui

import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.model.itemtypes.BaseItem

class FavoriteBadge(context: Context, val item: BaseItem? = null) : AppCompatImageView(context) {
	private val changeListener: () -> Unit = {
		item?.let(::setItemData)
	}

	init {
		setImageDrawable(context.getDrawable(R.drawable.ic_heart_red))
		item?.let(::setItemData)
	}

	constructor(context: Context) : this(context, null)

	private fun setItemData(item: BaseItem) {
		visibility = if (item.favorite) View.VISIBLE else View.GONE
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		item?.addChangeListener(changeListener)
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		item?.removeChangeListener(changeListener)
	}
}
