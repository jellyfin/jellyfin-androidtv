package org.jellyfin.androidtv.model.trailers.lifter

import android.util.Log
import org.jellyfin.androidtv.model.trailers.external.ExternalTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl
import java.net.MalformedURLException
import java.net.URL

abstract class BaseTrailerLifter {
	private val LOG_TAG = "BaseTrailerLifter"

	protected fun mediaUrlToUrl(mediaUrl: MediaUrl) = try {
		URL(mediaUrl.url)
	} catch (ex: MalformedURLException) {
		// TODO: Add logging back once DI is available, as directly referencing the Android Logger
		// TODO: causes the unit tests to fail as Log is not stubbed.
//		Log.i(LOG_TAG, "Failed to convert ${mediaUrl.url} to Url, retrying with https:// prepended")
		try {
			URL("https://" + mediaUrl.url)
		} catch (ex: MalformedURLException) {
//			Log.e(LOG_TAG, "Failed to convert ${mediaUrl.url} to Url")
			null
		}
	}

	abstract fun canLift(url: MediaUrl): Boolean
	abstract fun lift(url: MediaUrl): ExternalTrailer?
}
