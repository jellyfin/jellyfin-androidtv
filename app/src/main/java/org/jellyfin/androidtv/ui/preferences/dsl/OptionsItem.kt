package org.jellyfin.androidtv.ui.preferences.dsl

import androidx.preference.PreferenceCategory

interface OptionsItem {
	fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer)
}
