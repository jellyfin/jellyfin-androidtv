package org.jellyfin.androidtv.details.rows

import androidx.leanback.widget.Row
import org.jellyfin.androidtv.model.itemtypes.Season
import org.jellyfin.androidtv.model.itemtypes.Series

class SeriesMediaRow(
	val item: Series,
	var seasons: List<Season> = emptyList()
) : Row()
