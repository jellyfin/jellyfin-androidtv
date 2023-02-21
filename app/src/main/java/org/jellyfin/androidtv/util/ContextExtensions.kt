package org.jellyfin.androidtv.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.PluralsRes

/**
 * Get the activity hosting the current context
 */
tailrec fun Context.getActivity(): Activity? = when (this) {
	is Activity -> this
	else -> (this as? ContextWrapper)?.baseContext?.getActivity()
}

/**
 * Wrapper for getQuantityString in [resources] that supports all number types.
 * Note: Adds the [quantity] as first vararg, this is different behavior compared
 * to the platform function!
 */
fun Context.getQuantityString(@PluralsRes id: Int, quantity: Number, vararg args: Any) =
	resources.getQuantityString(id, quantity.toInt(), quantity, *args)
