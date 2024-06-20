package org.jellyfin.androidtv.ui

import org.jellyfin.sdk.model.api.BaseItemDto

fun BaseItemDto.copyWithTimerId(
	timerId: String,
) = copy(
	timerId = timerId,
)

fun BaseItemDto.copyWithSeriesTimerId(
	seriesTimerId: String,
) = copy(
	seriesTimerId = seriesTimerId,
)

