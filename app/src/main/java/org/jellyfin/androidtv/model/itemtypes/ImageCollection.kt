package org.jellyfin.androidtv.model.itemtypes

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.ImageOptions
import org.jellyfin.apiclient.model.entities.ImageType

class ImageCollection(original: BaseItemDto) {
	val primary = original.imageTags[ImageType.Primary]?.let { Image(original.id, ImageType.Primary, it) }
	val logo = original.imageTags[ImageType.Logo]?.let { Image(original.id, ImageType.Logo, it) }
	val backdrops = original.backdropImageTags.map { Image(original.id, ImageType.Backdrop, it) }.toList()
	val parentPrimary = original.parentPrimaryImageItemId?.let { Image(original.parentId, ImageType.Primary, it) }
	val parentBackdrops = original.parentBackdropImageTags?.let{ it.map {Image(original.parentBackdropItemId, ImageType.Backdrop, it)}.toList() }

	class Image(
		private val itemId: String,
		private val type: ImageType,
		private val tag: String?,
		private val index: Int? = null
	) {
		val url: String by lazy {
			TvApp.getApplication().apiClient.GetImageUrl(itemId, ImageOptions().also {
				it.imageType = type
				it.tag = tag
				it.imageIndex = index
				it.enableImageEnhancers = false
			})
		}

		suspend fun getBitmap(context: Context) = withContext(Dispatchers.IO) {
			Log.i("ImageCollection", "getBitmap() URL: $url")
			Picasso.with(context).load(url).get()
		}

		fun load(context: Context, success: (bitmap: Bitmap) -> Unit) {
			Log.i("ImageCollection", "load() URL: $url")
			GlobalScope.launch(Dispatchers.IO) {
				val bitmap = Picasso.with(context).load(url).get()
				withContext(Dispatchers.Main) { success(bitmap) }
			}
		}
	}
}
