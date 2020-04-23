package org.jellyfin.androidtv.details.rows

import androidx.leanback.widget.Row
import org.jellyfin.androidtv.details.actions.Action
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.model.itemtypes.ImageCollection

class DetailsOverviewRow(
	val item: BaseItem,
	val actions: List<Action>,
	val primaryImage: ImageCollection.Image?,
	val backdrops: List<ImageCollection.Image>
) : Row()
