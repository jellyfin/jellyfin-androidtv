package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.model.itemtypes.Episode
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType

fun BaseItemDto.liftToNewFormat() : BaseItem {
	return when(baseItemType) {
		BaseItemType.Episode -> Episode(this)
		else -> TODO()
	}
}
