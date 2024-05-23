package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import android.graphics.drawable.Drawable
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.data.model.ChapterItemInfo
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import java.util.UUID

abstract class BaseRowItem protected constructor(
	val baseRowType: BaseRowType,
	val staticHeight: Boolean = false,
	val preferParentThumb: Boolean = false,
	val selectAction: BaseRowItemSelectAction = BaseRowItemSelectAction.ShowDetails,
	val baseItem: BaseItemDto? = null,
	val chapterInfo: ChapterItemInfo? = null,
	val seriesTimerInfo: SeriesTimerInfoDto? = null,
	val gridButton: GridButton? = null,
) {
	open val itemId: UUID? = null
	open val baseItemType: BaseItemKind? = null
	open val showCardInfoOverlay: Boolean = false
	open val childCountStr: String? = null
	open val isFavorite: Boolean = false
	open val isPlayed: Boolean = false

	open fun getCardName(context: Context): String? = getFullName(context)

	open fun getImageUrl(
		context: Context,
		imageHelper: ImageHelper,
		imageType: ImageType,
		fillWidth: Int,
		fillHeight: Int,
	): String? = null

	open fun getFullName(context: Context): String? = null
	open fun getName(context: Context): String? = null
	open fun getSubText(context: Context): String? = null
	open fun getSummary(context: Context): String? = null
	open fun getBadgeImage(context: Context, imageHelper: ImageHelper): Drawable? = null

	override fun equals(other: Any?): Boolean {
		if (other is BaseRowItem) return other.itemId == itemId
		return super.equals(other)
	}
}
