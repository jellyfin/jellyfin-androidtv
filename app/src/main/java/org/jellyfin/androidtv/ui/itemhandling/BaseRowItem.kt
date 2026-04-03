package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

abstract class BaseRowItem protected constructor(
	val baseRowType: BaseRowType,
	val staticHeight: Boolean = false,
	val preferParentThumb: Boolean = false,
	val selectAction: BaseRowItemSelectAction = BaseRowItemSelectAction.ShowDetails,
	val baseItem: BaseItemDto? = null,
) {
	open val itemId: UUID? = null
	open val showCardInfoOverlay: Boolean = false
	open val isFavorite: Boolean = false
	open val isPlayed: Boolean = false

	open fun getCardName(context: Context): String? = getFullName(context)

	open fun getImage(imageType: ImageType): JellyfinImage? = null

	open fun getFullName(context: Context): String? = null
	open fun getName(context: Context): String? = null
	open fun getSubText(context: Context): String? = null
	open fun getSummary(context: Context): String? = null

	override fun equals(other: Any?): Boolean {
		if (other is BaseRowItem) return other.itemId == itemId
		return super.equals(other)
	}
}
