package org.jellyfin.androidtv.util.sdk

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import org.jellyfin.sdk.model.api.BaseItemDto

object TrailerUtils {
	private const val YOUTUBE_HOST = "youtube.com"
	private const val YOUTUBE_ID_PARAMETER = "v"
	private const val YOUTUBE_URL = "https://youtube.com/watch?v="
	private const val YOUTUBE_ID_LENGTH = 11
	private const val FRAMEWORK_STUB_PACKAGE = "com.android.tv.frameworkpackagestubs"

	@JvmStatic
	fun getExternalTrailerIntent(src: String): Intent {
		val uri = src.toUri()

		// Recreate YouTube urls
		if (uri.host?.endsWith(YOUTUBE_HOST) == true) {
			val id = uri.getQueryParameter(YOUTUBE_ID_PARAMETER).orEmpty()
			if (id.length == YOUTUBE_ID_LENGTH) return Intent(Intent.ACTION_VIEW, "$YOUTUBE_URL$id".toUri())
		}

		return Intent(Intent.ACTION_VIEW, uri)
	}

	@JvmStatic
	fun getExternalTrailerIntent(context: Context, item: BaseItemDto): Intent? = item.remoteTrailers.orEmpty()
		.mapNotNull { it.url?.let(::getExternalTrailerIntent) }
		.firstOrNull {
			val component = it.resolveActivity(context.packageManager)

			// Check if there is an activity to handle the intent
			// exclude the FrameworkPackageStubs module, which only displays a message
			// that there is no app to open the intent
			component != null && component.packageName != FRAMEWORK_STUB_PACKAGE
		}

	@JvmStatic
	fun hasPlayableTrailers(context: Context, item: BaseItemDto): Boolean {
		// Local trailer
		if (item.localTrailerCount != null && item.localTrailerCount!! > 0) return true

		// External trailer
		if (getExternalTrailerIntent(context, item) != null) return true

		// No trailer found
		return false
	}
}
