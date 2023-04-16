package org.jellyfin.androidtv.ui

import androidx.annotation.DrawableRes

open class GridButton @JvmOverloads constructor(
	val id: Int,
	val text: String,
	@DrawableRes val imageRes: Int? = null,
) {
	override fun toString() = text
}
