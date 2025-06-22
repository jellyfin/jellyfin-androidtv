package org.jellyfin.androidtv.ui.playback.stillwatching

import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

data class StillWatchingItemData(
	val baseItem: BaseItemDto,
	val id: UUID,
	val title: String,
	val thumbnail: JellyfinImage?,
	val logo: JellyfinImage?,
)
