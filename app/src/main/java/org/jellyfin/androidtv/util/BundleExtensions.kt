package org.jellyfin.androidtv.util

import android.os.Build
import android.os.Bundle

@Suppress("DEPRECATION")
inline fun <reified T> Bundle.getValue(key: String): T? = when {
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
	else -> get(key) as T?
}
