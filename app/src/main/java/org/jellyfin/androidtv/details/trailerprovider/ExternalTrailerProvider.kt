package org.jellyfin.androidtv.details.trailerprovider

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import org.jellyfin.androidtv.R
import org.jellyfin.apiclient.model.entities.MediaUrl

abstract class ExternalTrailerProvider(protected val context: Context) : TrailerProvider {

	private val domainRegex = Regex("""^(.*\.|)(.+\...+)$""")

	override fun getName(item: Any): String {
		return checkedDowncast(item).name
	}

	protected fun getDomain(uri: Uri): String? = uri.host?.let { host -> domainRegex.find(host)?.groups?.get(2)?.value }


	protected fun checkedDowncast(item: Any): MediaUrl {
		if (!canHandle(item))
			throw java.lang.IllegalArgumentException("Tried to pass a non-YouTube item to YouTube Provider!")
		return item as MediaUrl
	}

	override fun onClick(item: Any) {
		if (item !is MediaUrl)
			throw IllegalArgumentException("Tried to pass ExternalTrailerProvide.onClick a non-media-url")

		val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))

		if (intent.resolveActivity(context.packageManager) != null)
			context.startActivity(intent)
		else
			Toast.makeText(context, R.string.toast_no_trailer_handler, Toast.LENGTH_LONG).show()
	}
}
