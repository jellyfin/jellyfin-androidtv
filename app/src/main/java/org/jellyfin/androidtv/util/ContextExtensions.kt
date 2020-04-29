package org.jellyfin.androidtv.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Get the activity hosting the current context
 */
tailrec fun Context.getActivity(): Activity? = when (this) {
	is Activity -> this
	else -> (this as? ContextWrapper)?.baseContext?.getActivity()
}
