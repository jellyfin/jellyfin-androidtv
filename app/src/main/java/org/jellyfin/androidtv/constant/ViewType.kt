package org.jellyfin.androidtv.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class ViewType(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * Grid.
	 */
	GRID(R.string.view_type_grid),

	/**
	 * List.
	 */
	LIST(R.string.view_type_list),
}
