package org.jellyfin.androidtv.integration.dream

import android.graphics.drawable.Drawable
import org.jellyfin.sdk.model.api.BaseItemDto

data class LibraryDreamItem(
	val baseItem: BaseItemDto,
	val background: Drawable,
)
