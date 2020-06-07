package org.jellyfin.androidtv.preferences.ui.dsl

import androidx.annotation.StringRes

/**
 * Annotation used in the [OptionsItemEnum] to add display options to enum entries.
 * @param name The id of the name resource. -1 for "none" (default).
 * @param hidden True to hide this option or false (default) to display.
 */
annotation class EnumDisplayOptions(
	@StringRes val name: Int = -1,
	val hidden: Boolean = false
)
