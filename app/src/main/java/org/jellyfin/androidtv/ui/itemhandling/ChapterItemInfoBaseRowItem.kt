package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.data.model.ChapterItemInfo
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.sdk.model.extensions.ticks

class ChapterItemInfoBaseRowItem(
	val chapterInfo: ChapterItemInfo,
) : BaseRowItem(
	baseRowType = BaseRowType.Chapter,
	staticHeight = true,
) {
	override fun getImage(imageType: ImageType) = chapterInfo.image
	override val itemId get() = chapterInfo.itemId
	override fun getFullName(context: Context) = chapterInfo.name
	override fun getName(context: Context) = chapterInfo.name

	override fun getSubText(context: Context) =
		chapterInfo.startPositionTicks.ticks.inWholeMilliseconds.let(TimeUtils::formatMillis)
}
