package org.jellyfin.androidtv.ui.itemdetail

import androidx.leanback.widget.Row
import org.jellyfin.androidtv.data.model.InfoItem
import org.jellyfin.sdk.model.api.BaseItemDto

class MyDetailsOverviewRow @JvmOverloads constructor(
	val item: BaseItemDto,
	var imageDrawable: String? = null,
	var summary: String? = null,
	var progress: Int = 0,
	var infoItem1: InfoItem? = null,
	var infoItem2: InfoItem? = null,
	var infoItem3: InfoItem? = null,
) : Row()
