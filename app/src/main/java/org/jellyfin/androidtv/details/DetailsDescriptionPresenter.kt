package org.jellyfin.androidtv.details

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.bold
import androidx.leanback.widget.Presenter
import kotlinx.android.synthetic.main.row_details_description.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.model.itemtypes.PlayableItem
import org.jellyfin.androidtv.ui.RecyclerViewSpacingDecoration

class DetailsDescriptionPresenter : Presenter() {
	class ViewHolder(view: View) : Presenter.ViewHolder(view) {
		val title = view.findViewById<TextView>(R.id.details_description_title)
		val subtitle = view.findViewById<TextView>(R.id.details_description_subtitle)
		val body = view.details_description_body as TextView
		val year = view.details_description_year
		val rating = view.details_description_rating
		val genres = view.details_description_genres.apply {
			adapter = GenreAdapter()
			addItemDecoration(RecyclerViewSpacingDecoration(8))
		}
		val tags = view.details_description_tags
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

		// title
		viewHolder.title.text = item.title

		// Original title
		if (item.titleOriginal != null && item.titleOriginal != item.title) {
			viewHolder.subtitle.text = item.titleOriginal
			viewHolder.subtitle.visibility = View.VISIBLE
		} else {
			viewHolder.subtitle.visibility = View.GONE
		}

		// rating
		if (item is Movie) { //todo move those properties to baseitem or something
			viewHolder.year.text = item.productionYear.toString()
			viewHolder.rating.text = item.communityRating.toString()
		}

		if (item is PlayableItem)
			(viewHolder.genres.adapter as GenreAdapter).setItems(item.genres)

		if (item is PlayableItem && item.tags.isNotEmpty()) {
			viewHolder.tags.text = SpannableStringBuilder()
				.bold { append("Tags: ") }
				.append(item.tags.joinToString(", "))
			viewHolder.tags.visibility = View.VISIBLE
		} else {
			viewHolder.tags.visibility = View.GONE
		}

		// description
		viewHolder.body.text = item.description
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder?) {}
}
