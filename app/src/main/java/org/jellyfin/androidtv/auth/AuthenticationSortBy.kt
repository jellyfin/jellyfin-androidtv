package org.jellyfin.androidtv.auth

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

enum class AuthenticationSortBy {
	@EnumDisplayOptions(name = R.string.last_use)
	LAST_USE,
	@EnumDisplayOptions(name = R.string.alphabetical)
	ALPHABETICAL
}
