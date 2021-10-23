package org.jellyfin.androidtv.ui.itempreview

import android.text.SpannableStringBuilder
import org.jellyfin.sdk.model.api.BaseItemDto

data class ItemPreviewItemData(
	val baseItem: BaseItemDto,
	val title: String,
	val numbersString: SpannableStringBuilder,
	val subtitle: String?,
	val logoImageUrl: String?,
	val rowHeader: String
)
