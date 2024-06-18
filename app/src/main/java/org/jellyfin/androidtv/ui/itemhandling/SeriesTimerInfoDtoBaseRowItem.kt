package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.getSeriesOverview
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

class SeriesTimerInfoDtoBaseRowItem(
	val seriesTimerInfo: SeriesTimerInfoDto,
) : BaseRowItem(
	baseRowType = BaseRowType.SeriesTimer,
) {
	override fun getImageUrl(
		context: Context,
		imageHelper: ImageHelper,
		imageType: ImageType,
		fillWidth: Int,
		fillHeight: Int
	) = imageHelper.getResourceUrl(
		context,
		R.drawable.tile_land_series_timer
	)

	override fun getFullName(context: Context) = seriesTimerInfo.name
	override fun getName(context: Context) = seriesTimerInfo.name
	override val itemId get() = seriesTimerInfo.id?.toUUIDOrNull()
	override fun getSubText(context: Context): String = listOfNotNull(
		if (seriesTimerInfo.recordAnyChannel == true) context.getString(R.string.all_channels)
		else seriesTimerInfo.channelName,
		seriesTimerInfo.dayPattern
	).joinToString(" ")

	override fun getSummary(context: Context) = seriesTimerInfo.getSeriesOverview(context)
}
