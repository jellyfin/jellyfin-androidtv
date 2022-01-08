package org.jellyfin.androidtv.ui.preference.dsl

import org.jellyfin.androidtv.preference.IPreferenceStore
import org.jellyfin.androidtv.preference.Preference
import org.jellyfin.androidtv.preference.PreferenceVal

abstract class OptionsItemMutable<T : Any> : OptionsItem {
	var title: String? = null
	var enabled: Boolean = true
	var visible: Boolean = true

	protected var dependencyCheckFun: () -> Boolean = { true }
	protected lateinit var binder: OptionsBinder<T>

	open fun bind(store: IPreferenceStore, preference: Preference<T>) = bind {
		get { store[preference] }
		set { store[preference] = PreferenceVal.buildBasedOnT(it) }
		default { store.getDefaultValue(preference) }
	}

	open fun bind(init: OptionsBinder.Builder<T>.() -> Unit) {
		this.binder = OptionsBinder.Builder<T>()
			.apply { init() }
			.build()
	}

	fun depends(dependencyCheckFun: () -> Boolean) {
		this.dependencyCheckFun = dependencyCheckFun
	}
}
