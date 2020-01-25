package org.jellyfin.androidtv.model.itemtypes

import org.jellyfin.apiclient.model.dto.BaseItemDto

abstract class PlayableItem(original: BaseItemDto) : BaseItem(original) {
	val canResume: Boolean
	val playbackPositionTicks: Long

	init {
		canResume = original.canResume
		playbackPositionTicks = original.userData.playbackPositionTicks
	}
}
