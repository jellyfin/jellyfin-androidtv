package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum
import org.jellyfin.sdk.model.constant.ItemSortBy

enum class LiveTvChannelOrder(
	override val nameRes: Int,
	val stringValue: String,
) : PreferenceEnum {
	LAST_PLAYED(R.string.lbl_guide_option_played, ItemSortBy.DatePlayed),
	CHANNEL_NUMBER(R.string.lbl_guide_option_number, ItemSortBy.SortName);

	companion object {
		fun fromString(value: String) = values()
			.firstOrNull { it.stringValue.equals(value, ignoreCase = true) }
			?: LAST_PLAYED
	}
}
