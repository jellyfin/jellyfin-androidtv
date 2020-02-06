package org.jellyfin.androidtv.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.model.itemtypes.BaseItem

class DetailsDescriptionPresenter : Presenter() {
	/**
	 * The ViewHolder for the [AbstractDetailsDescriptionPresenter].
	 */
	class ViewHolder(view: View) : Presenter.ViewHolder(view) {
		val title: TextView = view.findViewById(R.id.details_description_title)
		val subtitle: TextView = view.findViewById(R.id.details_description_subtitle)
		val body: TextView = view.findViewById(R.id.details_description_body)
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = LayoutInflater
			.from(parent.context)
			.inflate(R.layout.row_details_description, parent, false)

		return ViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
		viewHolder as ViewHolder
		item as BaseItem

		viewHolder.title.text = item.name
		viewHolder.subtitle.text = item.name
		viewHolder.body.text = item.description
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder?) {}
}
