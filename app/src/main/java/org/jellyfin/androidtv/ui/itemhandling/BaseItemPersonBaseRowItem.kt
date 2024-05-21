package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.sdk.model.api.BaseItemPerson

class BaseItemPersonBaseRowItem(
	item: BaseItemPerson,
) : BaseRowItem(
	baseRowType = BaseRowType.Person,
	staticHeight = true,
	basePerson = item,
) {
	override fun getPrimaryImageUrl(
		context: Context,
		fillHeight: Int,
	) = imageHelper.getPrimaryImageUrl(basePerson!!, fillHeight)

	override fun getItemId() = basePerson?.id
	override fun getFullName(context: Context) = basePerson?.name
	override fun getName(context: Context) = basePerson?.name
	override fun getSubText(context: Context) = basePerson?.role
}
