package org.jellyfin.androidtv.preferences.ui.dsl

import androidx.preference.PreferenceCategory

abstract class OptionsItem {
	abstract fun build(category: PreferenceCategory)
}
