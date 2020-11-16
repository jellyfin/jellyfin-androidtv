package org.jellyfin.androidtv.ui

import androidx.annotation.DrawableRes

open class GridButton(
	val id: Int,
	val text: String,
	@DrawableRes val imageIndex: Int
) {
	override fun toString() = text
}
