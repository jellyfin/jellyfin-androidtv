package org.jellyfin.androidtv.util

fun <T> Collection<T>.randomOrNull(): T? {
	return if (isNullOrEmpty()) null else random()
}
