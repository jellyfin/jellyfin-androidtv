package org.jellyfin.androidtv.integration.provider

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.error
import coil3.toBitmap
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.AndroidVersion
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException

class ImageProvider : ContentProvider() {
	private val imageLoader by inject<ImageLoader>()

	override fun onCreate(): Boolean = true

	override fun getType(uri: Uri) = MIME_TYPE

	override fun query(
		uri: Uri,
		projection: Array<out String>?,
		selection: String?,
		selectionArgs: Array<out String>?,
		sortOrder: String?
	) = null

	override fun insert(uri: Uri, values: ContentValues?): Uri =
		throw UnsupportedOperationException("Images are read-only")

	override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
		throw UnsupportedOperationException("Images are read-only")

	override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
		throw UnsupportedOperationException("Images are read-only")

	override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
		if (mode != "r") throw FileNotFoundException("Images can only be opened for reading")

		val source = uri.getQueryParameter(SOURCE_PARAMETER)
			?.takeIf { it.isNotBlank() }
			?.toUri()
			?.normalizeScheme()
			?: throw FileNotFoundException("Missing image source")

		if (source.scheme !in setOf("http", "https", ContentResolver.SCHEME_ANDROID_RESOURCE) || source.authority.isNullOrBlank()) {
			throw FileNotFoundException("Unsupported image source")
		}

		return openPipeHelper(uri, MIME_TYPE, null, source) { output, _, _, _, _ ->
			try {
				ParcelFileDescriptor.AutoCloseOutputStream(output).use { stream ->
					val context = requireNotNull(context)
					val result = runBlocking {
						imageLoader.execute(ImageRequest.Builder(context).apply {
							data(source)
							allowHardware(false)
							error(R.drawable.placeholder_icon)
						}.build())
					}
					if (result is ErrorResult) Timber.w(result.throwable, "Unable to load image $source")
					val bitmap = requireNotNull(result.image).toBitmap()

					@Suppress("DEPRECATION")
					val format = if (AndroidVersion.isAtLeastR) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
					check(bitmap.compress(format, COMPRESSION_QUALITY, stream)) {
						"Unable to compress image"
					}
				}
			} catch (_: IOException) {
				// The requester may close the pipe before the image has been written.
			} catch (error: Exception) {
				Timber.w(error, "Unable to write provider image")
			}
		}
	}

	companion object {
		private const val MIME_TYPE = "image/webp"
		private const val SOURCE_PARAMETER = "src"
		private const val COMPRESSION_QUALITY = 95

		/**
		 * Get a [Uri] that uses the [ImageProvider] to load an image. The input should be a valid
		 * Jellyfin image URL created using the SDK, or an android.resource URL for fallback artwork.
		 */
		fun getImageUri(src: String): Uri = Uri.Builder()
			.scheme("content")
			.authority("${BuildConfig.APPLICATION_ID}.integration.provider.ImageProvider")
			.appendQueryParameter(SOURCE_PARAMETER, src)
			.appendQueryParameter("v", BuildConfig.VERSION_NAME)
			.build()
	}
}
