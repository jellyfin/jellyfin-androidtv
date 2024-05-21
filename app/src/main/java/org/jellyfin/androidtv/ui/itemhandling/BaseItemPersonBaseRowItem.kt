package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.sdk.model.api.BaseItemPerson

class BaseItemPersonBaseRowItem(
	val person: BaseItemPerson,
) : BaseRowItem(
	baseRowType = BaseRowType.Person,
	staticHeight = true,
) {
	override fun getPrimaryImageUrl(
		context: Context,
		fillHeight: Int,
	) = imageHelper.getPrimaryImageUrl(person, fillHeight)

	override fun getItemId() = person.id
	override fun getFullName(context: Context) = person.name
	override fun getName(context: Context) = person.name
	override fun getSubText(context: Context) = person.role
}
