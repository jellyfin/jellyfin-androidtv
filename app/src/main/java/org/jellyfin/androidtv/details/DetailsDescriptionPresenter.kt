package org.jellyfin.androidtv.details

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter

class DetailsDescriptionPresenter(val title: String, val subtitle: String, val body: String) : AbstractDetailsDescriptionPresenter() {
	override fun onBindDescription(viewHolder: ViewHolder, itemData: Any) {

		viewHolder.also {
			it.title.text = title
			it.subtitle.text = subtitle
			it.body.text = body
		}
	}
}
