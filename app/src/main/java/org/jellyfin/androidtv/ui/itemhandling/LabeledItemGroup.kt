package org.jellyfin.androidtv.ui.itemhandling

import androidx.annotation.StringRes
import org.jellyfin.sdk.model.api.BaseItemDto

data class LabeledItemGroup(
	@StringRes val labelRes: Int,
	val items: Collection<BaseItemDto>,
)
