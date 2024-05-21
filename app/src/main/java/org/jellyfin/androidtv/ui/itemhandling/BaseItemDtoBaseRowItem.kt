package org.jellyfin.androidtv.ui.itemhandling

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

open class BaseItemDtoBaseRowItem @JvmOverloads constructor(
	index: Int = 0,
	item: BaseItemDto,
	preferParentThumb: Boolean = false,
	staticHeight: Boolean = false,
	selectAction: BaseRowItemSelectAction = BaseRowItemSelectAction.ShowDetails,
	preferSeriesPoster: Boolean = false,
) : BaseRowItem(
	baseRowType = when (item.type) {
		BaseItemKind.TV_CHANNEL -> BaseRowType.LiveTvChannel
		BaseItemKind.PROGRAM -> BaseRowType.LiveTvProgram
		BaseItemKind.RECORDING -> BaseRowType.LiveTvRecording
		else -> BaseRowType.BaseItem
	},
	index = index,
	staticHeight = staticHeight,
	preferParentThumb = preferParentThumb,
	selectAction = selectAction,
	baseItem = item,
	preferSeriesPoster = preferSeriesPoster,
)
