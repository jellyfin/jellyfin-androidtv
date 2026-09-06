package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/**
 * How an HDR format should be treated in the device profile, overriding device-reported capabilities.
 */
enum class HdrOverrideMode(
	override val nameRes: Int,
) : PreferenceEnum {
	AUTO(R.string.hdr_override_auto),
	ENABLE(R.string.hdr_override_enable),
	DISABLE(R.string.hdr_override_disable),
}
