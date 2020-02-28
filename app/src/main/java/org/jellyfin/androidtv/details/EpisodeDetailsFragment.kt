package org.jellyfin.androidtv.details

import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.model.itemtypes.Episode

class EpisodeDetailsFragment(item: Episode) : BaseDetailsFragment<Episode>(item) {
	private val detailsRow by lazy { DetailsOverviewRow("") }

	override fun onCreateAdapter(adapter: StateObjectAdapter<Row>, selector: ClassPresenterSelector) {
		super.onCreateAdapter(adapter, selector)

		adapter.add(detailsRow)
	}

	override suspend fun setItem(item: Episode) {
		super.setItem(item)

		// Update detail row
		detailsRow.item = item
		detailsRow.setImageBitmap(context!!, item.images.primary?.getBitmap(context!!))
	}
}
