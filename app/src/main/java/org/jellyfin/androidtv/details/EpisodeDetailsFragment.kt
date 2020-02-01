package org.jellyfin.androidtv.details

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import org.jellyfin.androidtv.model.itemtypes.Episode

class EpisodeDetailsFragment(item: Episode) : BaseDetailsFragment<Episode>(item) {
	private val detailsRow by lazy { DetailsOverviewRow("") }

	override fun onCreateAdapter(adapter: ArrayObjectAdapter, selector: ClassPresenterSelector) {
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
