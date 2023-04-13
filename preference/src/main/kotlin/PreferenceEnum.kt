package org.jellyfin.preference

import androidx.annotation.StringRes

interface PreferenceEnum {
	/**
	 * True to hide this option or false (default) to display.
	 */
	val hidden: Boolean get() = false

	/**
	 * The id of the name resource or -1 for "none".
	 */
	@get:StringRes
	val nameRes: Int

	/**
	 * The name used to store the preference or null to use the name property.
	 */
	val serializedName: String? get() = null
}
