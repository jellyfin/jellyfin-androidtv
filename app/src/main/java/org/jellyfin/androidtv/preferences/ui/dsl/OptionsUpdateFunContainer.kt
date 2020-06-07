package org.jellyfin.androidtv.preferences.ui.dsl

typealias OptionsUpdateFun = () -> Unit

class OptionsUpdateFunContainer {
	private val callbacks = mutableSetOf<OptionsUpdateFun>()

	operator fun plusAssign(callback: OptionsUpdateFun) {
		callbacks += callback
	}

	operator fun invoke() {
		callbacks.forEach {
			it.invoke()
		}
	}
}
