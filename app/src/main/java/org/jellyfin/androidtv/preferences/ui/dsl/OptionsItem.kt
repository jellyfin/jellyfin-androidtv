package org.jellyfin.androidtv.preferences.ui.dsl

import androidx.preference.PreferenceCategory

interface OptionsItem {
	fun build(category: PreferenceCategory)
}
