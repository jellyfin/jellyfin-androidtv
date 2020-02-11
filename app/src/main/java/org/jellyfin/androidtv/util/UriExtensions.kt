package org.jellyfin.androidtv.util

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.AnyRes
import org.jellyfin.androidtv.TvApp

/**
 * Get the uri for a given resource
 */
fun uriFromResourceId(@AnyRes resid: Int): Uri {
	val resources = TvApp.getApplication().applicationContext.resources

	return Uri.parse(StringBuilder().apply {
		// Protocol
		append(ContentResolver.SCHEME_ANDROID_RESOURCE)
		append("://")
		append(resources.getResourcePackageName(resid))
		append("/")
		append(resources.getResourceTypeName(resid))
		append("/")
		append(resources.getResourceEntryName(resid))
	}.toString())
}
