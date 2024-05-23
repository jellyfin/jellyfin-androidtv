package org.jellyfin.androidtv.ui.itemhandling

import org.jellyfin.sdk.model.api.BaseItemDto

class AudioQueueBaseRowItem(
	index: Int,
	item: BaseItemDto,
) : BaseItemDtoBaseRowItem(
	item = item,
	staticHeight = true,
) {
	var playing: Boolean = false
}
