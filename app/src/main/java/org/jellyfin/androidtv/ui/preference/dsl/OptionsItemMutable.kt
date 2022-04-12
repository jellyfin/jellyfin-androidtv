package org.jellyfin.androidtv.ui.preference.dsl

import org.jellyfin.preference.Preference
import org.jellyfin.preference.store.PreferenceStore

abstract class OptionsItemMutable<T : Any> : OptionsItem {
	var title: String? = null
	var enabled: Boolean = true
	var visible: Boolean = true

	protected var dependencyCheckFun: () -> Boolean = { true }
	protected lateinit var binder: OptionsBinder<T>

	open fun bind(store: PreferenceStore<*, *>, preference: Preference<T>) = bind {
		get { store[preference] }
		set { store[preference] = it }
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
