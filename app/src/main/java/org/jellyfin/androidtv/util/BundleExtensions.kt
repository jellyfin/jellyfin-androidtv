package org.jellyfin.androidtv.util

import android.os.Bundle

@Suppress("DEPRECATION")
inline fun <reified T> Bundle.getValue(key: String): T? = when {
	AndroidVersion.isAtLeastT -> getParcelable(key, T::class.java)
	else -> get(key) as T?
}

fun createBundle(init: (Bundle.() -> Unit)? = null) = Bundle().also { bundle ->
	if (init != null) bundle.init()
}
