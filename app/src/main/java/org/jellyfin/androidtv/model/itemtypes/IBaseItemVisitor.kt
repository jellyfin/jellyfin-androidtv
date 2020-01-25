package org.jellyfin.androidtv.model.itemtypes

interface IBaseItemVisitor {
	fun visit(item: Episode)
}
