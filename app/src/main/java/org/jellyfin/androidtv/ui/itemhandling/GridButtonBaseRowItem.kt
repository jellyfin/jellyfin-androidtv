package org.jellyfin.androidtv.ui.itemhandling

import org.jellyfin.androidtv.ui.GridButton

class GridButtonBaseRowItem(
	item: GridButton,
) : BaseRowItem(
	baseRowType = BaseRowType.GridButton,
	staticHeight = true,
	gridButton = item,
)
