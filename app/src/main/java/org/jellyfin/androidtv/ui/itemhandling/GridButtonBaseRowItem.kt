package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.androidtv.ui.GridButton

class GridButtonBaseRowItem(
	item: GridButton,
) : BaseRowItem(
	baseRowType = BaseRowType.GridButton,
	staticHeight = true,
	gridButton = item,
) {
	override fun getPrimaryImageUrl(
		context: Context,
		fillHeight: Int,
	) = gridButton?.imageRes?.let {
		imageHelper.getResourceUrl(context, it)
	}

	override fun getFullName(context: Context) = gridButton?.text
	override fun getName(context: Context) = gridButton?.text
}
