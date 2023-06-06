package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum
import org.jellyfin.sdk.model.api.ItemSortBy

enum class LiveTvChannelOrder(
	override val nameRes: Int,
	val stringValue: String,
) : PreferenceEnum {
	LAST_PLAYED(R.string.lbl_guide_option_played, ItemSortBy.DATE_PLAYED.serialName),
	CHANNEL_NUMBER(R.string.lbl_guide_option_number, ItemSortBy.SORT_NAME.serialName);

	companion object {
		fun fromString(value: String) = entries
			.firstOrNull { it.stringValue.equals(value, ignoreCase = true) }
			?: LAST_PLAYED
	}
}
