package org.jellyfin.androidtv.model.itemtypes

import android.graphics.Bitmap
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType

class Episode(original: BaseItemDto) : PlayableItem(original) {
	//TODO: Chapters: ArrayList<ChapterInfoDto>
	val communityRating: Double

	override fun acceptVisitor(visitor: IBaseItemVisitor) {
		visitor.visit(this)
	}

	init {
		if (original.baseItemType != BaseItemType.Episode) {
			throw IllegalArgumentException("Tried to create an Episode from a non-Episode BaseItemDto")
		}

	    communityRating = original.communityRating.toDouble()
	}
}
