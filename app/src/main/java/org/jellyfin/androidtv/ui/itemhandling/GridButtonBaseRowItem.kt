package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.util.ImageHelper

class GridButtonBaseRowItem(
	item: GridButton,
) : BaseRowItem(
	baseRowType = BaseRowType.GridButton,
	staticHeight = true,
	gridButton = item,
) {
	override fun getImageUrl(
		context: Context,
		imageHelper: ImageHelper,
		imageType: ImageType,
		fillWidth: Int,
		fillHeight: Int
	) = gridButton?.imageRes?.let {
		imageHelper.getResourceUrl(context, it)
	}

	override fun getFullName(context: Context) = gridButton?.text
	override fun getName(context: Context) = gridButton?.text
}
