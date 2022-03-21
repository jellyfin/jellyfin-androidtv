package org.jellyfin.androidtv.ui

fun interface ValueChangedListener<T> {
	fun onValueChanged(value: T)
}
