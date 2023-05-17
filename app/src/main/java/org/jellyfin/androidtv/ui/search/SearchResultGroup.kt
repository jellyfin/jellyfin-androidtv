package org.jellyfin.androidtv.ui.search

import androidx.annotation.StringRes
import org.jellyfin.sdk.model.api.BaseItemDto

data class SearchResultGroup(
	@StringRes val labelRes: Int,
	val items: Collection<BaseItemDto>,
)
