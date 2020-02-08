package org.jellyfin.androidtv.base

interface IItemClickListener {
	suspend fun onItemClicked(item: Any?)
}
