package org.jellyfin.androidtv.preference

abstract class BasicPreferenceStore : IPreferenceStore {
	
	override fun <T : Any> getDefaultValue(preference: Preference<T>): T {
		return preference.defaultValue.data
	}

	override fun <T : Any> reset(preference: Preference<T>) {
		this[preference] = PreferenceVal.buildBasedOnT(getDefaultValue(preference))
	}
}
