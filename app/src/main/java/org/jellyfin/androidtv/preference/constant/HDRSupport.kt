package org.jellyfin.androidtv.preference.constant

import androidx.annotation.StringRes
import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class HDRSupport(
	override val nameRes: Int,
	@get:StringRes val descriptionRes: Int,
) : PreferenceEnum {
	AUTOMATIC(R.string.hdr_support_automatic, R.string.hdr_support_automatic_description),
	ENABLED(R.string.hdr_support_enabled, R.string.hdr_support_enabled_description),
	DISABLED(R.string.state_disabled, R.string.hdr_support_disabled_description),
}
