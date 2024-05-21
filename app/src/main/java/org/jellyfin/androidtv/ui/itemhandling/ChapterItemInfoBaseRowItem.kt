package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.androidtv.data.model.ChapterItemInfo
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.sdk.model.extensions.ticks

class ChapterItemInfoBaseRowItem(
	item: ChapterItemInfo,
) : BaseRowItem(
	baseRowType = BaseRowType.Chapter,
	staticHeight = true,
	chapterInfo = item,
) {
	override fun getPrimaryImageUrl(
		context: Context,
		fillHeight: Int,
	) = chapterInfo?.imagePath

	override fun getItemId() = chapterInfo?.itemId
	override fun getFullName(context: Context) = chapterInfo?.name
	override fun getName(context: Context) = chapterInfo?.name

	override fun getSubText(context: Context) =
		chapterInfo?.startPositionTicks?.ticks?.inWholeMilliseconds?.let(TimeUtils::formatMillis)
}
