package org.jellyfin.androidtv.ui

import androidx.annotation.DrawableRes

open class GridButton(
	val id: Int,
	val text: String,
	@DrawableRes val imageRes: Int,
	val imageUrl: String? = null
) {
	override fun toString() = text
}
