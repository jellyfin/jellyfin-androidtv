package org.jellyfin.androidtv.model.itemtypes

import org.jellyfin.apiclient.model.dto.BaseItemDto

abstract class BaseItem(original: BaseItemDto) {
	val id: String
	val canResume: Boolean
	val playbackPositionTicks: Long
	val name: String
	val description: String

	init {
	    id = original.id
		canResume = original.canResume
		playbackPositionTicks = original.userData.playbackPositionTicks
		name = original.name
		description = original.overview
	}

	abstract fun acceptVisitor(visitor: IBaseItemVisitor)
}
