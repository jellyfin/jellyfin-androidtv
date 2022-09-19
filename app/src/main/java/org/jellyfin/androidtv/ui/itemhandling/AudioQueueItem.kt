package org.jellyfin.androidtv.ui.itemhandling

import org.jellyfin.apiclient.model.dto.BaseItemDto

class AudioQueueItem(
	index: Int,
	item: BaseItemDto,
) : BaseRowItem(
	index = index,
	item = item,
	staticHeight = true
)
