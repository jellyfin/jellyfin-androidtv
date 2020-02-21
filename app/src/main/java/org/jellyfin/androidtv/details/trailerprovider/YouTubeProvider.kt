package org.jellyfin.androidtv.details.trailerprovider

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import org.jellyfin.androidtv.R
import org.jellyfin.apiclient.model.entities.MediaUrl

class YouTubeProvider(context: Context, private val safeBrandingCompliance: Boolean) : ExternalTrailerProvider(context) {
	private val youtubeDomains = arrayListOf("youtube.com", "youtu.be")

	private fun isYoutubeUrl(uri: Uri) = youtubeDomains.contains(getDomain(uri))


	override fun canHandle(item: Any): Boolean {
		return item is MediaUrl && isYoutubeUrl(Uri.parse(item.url))
	}

	override fun getPlaceholder(): Drawable? {
		return context.getDrawable(R.drawable.banner_youtube)
	}

	override fun getIcon(): Drawable? {
		return if (safeBrandingCompliance) {
			context.getDrawable(R.drawable.ic_youtube)
		} else
			null
	}

	override fun getDescription(item: Any): String? {
		return context.getString(R.string.youtube)
	}

	override fun loadThumbnail(item: Any, success: (Drawable) -> Unit) {
		if (safeBrandingCompliance)
			return
		val mediaUrl = checkedDowncast(item)



	}
}
