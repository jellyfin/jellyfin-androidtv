package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions
import org.jellyfin.apiclient.model.querying.ItemSortBy

enum class LiveTvChannelOrder(
	val stringValue: String
) {
	@EnumDisplayOptions(R.string.lbl_guide_option_played)
	LAST_PLAYED(ItemSortBy.DatePlayed),

	@EnumDisplayOptions(R.string.lbl_guide_option_number)
	CHANNEL_NUMBER(ItemSortBy.SortName);

	companion object {
		fun fromString(value: String) = values()
			.firstOrNull { it.stringValue.equals(value, ignoreCase = true) }
			?: LAST_PLAYED
	}
}
