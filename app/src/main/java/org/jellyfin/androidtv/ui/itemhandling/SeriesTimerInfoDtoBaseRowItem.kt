package org.jellyfin.androidtv.ui.itemhandling

import org.jellyfin.sdk.model.api.SeriesTimerInfoDto

class SeriesTimerInfoDtoBaseRowItem(
	item: SeriesTimerInfoDto,
) : BaseRowItem(
	baseRowType = BaseRowType.SeriesTimer,
	seriesTimerInfo = item,
)
