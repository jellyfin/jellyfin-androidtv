package org.jellyfin.androidtv.details.rows

import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ObjectAdapter

class VerticalListRow(
	header: HeaderItem,
	adapter: ObjectAdapter
) : ListRow(header, adapter)
