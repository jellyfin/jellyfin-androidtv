package org.jellyfin.androidtv.details

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter

class DetailsDescriptionPresenter(val title: String, val subtitle: String, val body: String) : AbstractDetailsDescriptionPresenter() {
	override fun onBindDescription(viewHolder: ViewHolder, itemData: Any) {
		val us = this

		viewHolder.apply {
			title.text = us.title
			subtitle.text = us.subtitle
			body.text = us.body
		}
	}
}
