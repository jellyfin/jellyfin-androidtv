package org.jellyfin.androidtv.model.itemtypes

import org.jellyfin.apiclient.model.dto.ChapterInfoDto
import org.jellyfin.apiclient.model.entities.ImageType

class ChapterInfo(originalChapterInfoDto: ChapterInfoDto, val baseItem: BaseItem, index: Int) {
	val startPositionTicks: Long = originalChapterInfoDto.startPositionTicks / 10000
	val name: String = originalChapterInfoDto.name
	val image: ImageCollection.Image? = originalChapterInfoDto.imageTag?.let { ImageCollection.Image(baseItem.id, ImageType.Chapter, it, index) }
}
