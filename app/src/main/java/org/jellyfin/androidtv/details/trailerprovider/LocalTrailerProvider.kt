package org.jellyfin.androidtv.details.trailerprovider

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.details.DetailsActivity
import org.jellyfin.androidtv.model.itemtypes.BaseItem

class LocalTrailerProvider(private val context: Context) : TrailerProvider {
	override fun canHandle(item: Any): Boolean {
		return item is BaseItem
	}

	override fun getPlaceholder(): Drawable? {
		return context.getDrawable(R.drawable.tile_port_video)
	}

	override fun getIcon(): Drawable? {
		return null
	}

	override fun getName(item: Any): String {
		return checkedDowncast(item).title
	}

	override fun getDescription(item: Any): String? {
		return context.getString(R.string.lbl_local_trailer)
	}

	override fun loadThumbnail(item: Any, success: (Drawable) -> Unit) {

	}

	private fun checkedDowncast(item: Any): BaseItem {
		if (!canHandle(item))
			throw IllegalArgumentException("Tried to pass an unsupported item to TrailerProvider!")
		return item as BaseItem
	}

	override fun onClick(item: Any) {
		val baseItem = checkedDowncast(item)
		val intent = Intent(context, DetailsActivity::class.java)
		intent.putExtra("id", baseItem.id)
		context.startActivity(intent)
	}

}
