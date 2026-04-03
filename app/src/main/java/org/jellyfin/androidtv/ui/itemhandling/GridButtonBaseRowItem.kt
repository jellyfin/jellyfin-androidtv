package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.GridButton

class GridButtonBaseRowItem(
	val gridButton: GridButton,
) : BaseRowItem(
	baseRowType = BaseRowType.GridButton,
	staticHeight = true,
) {
	override fun getImage(imageType: ImageType) = null
	override fun getFullName(context: Context) = gridButton.text
	override fun getName(context: Context) = gridButton.text
}
