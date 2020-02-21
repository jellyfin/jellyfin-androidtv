package org.jellyfin.androidtv.details.trailerprovider

import android.graphics.drawable.Drawable

interface TrailerProvider {
	fun canHandle(item: Any): Boolean
	fun getPlaceholder(): Drawable?
	fun getIcon(): Drawable?
	fun getName(item: Any): String
	fun getDescription(item: Any): String?
	fun loadThumbnail(item: Any, success: (Drawable) -> Unit)
	fun onClick(item: Any)
}
