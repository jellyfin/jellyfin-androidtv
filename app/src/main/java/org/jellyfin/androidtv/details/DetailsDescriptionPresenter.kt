package org.jellyfin.androidtv.details

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import org.jellyfin.androidtv.model.itemtypes.BaseItem

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {
	override fun onBindDescription(viewHolder: ViewHolder, itemData: Any) {
		val item = itemData as BaseItem

		viewHolder.also {
			it.title.text = item.name
//			it.subtitle.text = subtitle
			it.body.text = item.description
		}
	}
}
