package org.jellyfin.androidtv.ui.itemhandling

import org.jellyfin.sdk.model.api.BaseItemPerson

class BaseItemPersonBaseRowItem(
	item: BaseItemPerson,
) : BaseRowItem(
	baseRowType = BaseRowType.Person,
	staticHeight = true,
	basePerson = item,
)
