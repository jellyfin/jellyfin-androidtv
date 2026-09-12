package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class SkipMode(
	override val nameRes: Int,
	val descriptionRes: Int,
) : PreferenceEnum {
	ABSOLUTE(R.string.skip_mode_absolute, R.string.skip_mode_absolute_description),
	RELATIVE(R.string.skip_mode_relative, R.string.skip_mode_relative_description),
}
