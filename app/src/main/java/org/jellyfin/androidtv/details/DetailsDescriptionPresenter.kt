package org.jellyfin.androidtv.details

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.bold
import androidx.leanback.widget.Presenter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.row_details_description.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.model.itemtypes.PlayableItem
import org.jellyfin.androidtv.ui.RecyclerViewSpacingDecoration
import org.jellyfin.androidtv.ui.widget.Rating

class DetailsDescriptionPresenter : Presenter() {
	class ViewHolder(view: View) : Presenter.ViewHolder(view) {
		val title: TextView = view.findViewById(R.id.details_description_title)
		val subtitle: TextView = view.findViewById(R.id.details_description_subtitle)
		val body: TextView = view.details_description_body
		val year: TextView = view.details_description_year
		val officialRating: TextView = view.details_description_official_rating
		val communityRating: Rating = view.details_description_community_rating
		val criticsRating: Rating = view.details_description_critics_rating
		val genres: RecyclerView = view.details_description_genres.apply {
			adapter = GenreAdapter()
			addItemDecoration(RecyclerViewSpacingDecoration(8))
		}
		val tags: TextView = view.details_description_tags
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

			if (item.officialRating != null) {
				viewHolder.officialRating.text = item.officialRating
				viewHolder.officialRating.visibility = View.VISIBLE
			} else {
				viewHolder.officialRating.visibility = View.GONE
			}

			viewHolder.communityRating.value = item.communityRating

			if (item.criticsRating != null) {
				viewHolder.criticsRating.value = item.criticsRating
				viewHolder.criticsRating.visibility = View.VISIBLE
			} else {
				viewHolder.criticsRating.visibility = View.GONE
			}
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
