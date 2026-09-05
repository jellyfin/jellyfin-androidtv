package org.jellyfin.androidtv.ui.favorites

import androidx.annotation.StringRes
import org.jellyfin.sdk.model.api.BaseItemDto

data class FavoritesResultGroup(
	@StringRes val labelRes: Int,
	val items: Collection<BaseItemDto>,
)
