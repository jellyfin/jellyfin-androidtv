package org.jellyfin.androidtv.ui.itemhandling

import org.jellyfin.androidtv.data.model.ChapterItemInfo

class ChapterItemInfoBaseRowItem(
	item: ChapterItemInfo,
) : BaseRowItem(
	baseRowType = BaseRowType.Chapter,
	staticHeight = true,
	chapterInfo = item,
)
