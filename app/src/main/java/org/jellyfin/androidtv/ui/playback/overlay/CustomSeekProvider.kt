package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.leanback.widget.PlaybackSeekDataProvider
import coil3.ImageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.maxBitmapSize
import coil3.request.transformations
import coil3.size.Dimension
import coil3.size.Size
import coil3.toBitmap
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.coil.SubsetTransformation
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.trickplayApi
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import kotlin.math.ceil
import kotlin.math.min

class CustomSeekProvider(
	private val videoPlayerAdapter: VideoPlayerAdapter,
	private val imageLoader: ImageLoader,
	private val api: ApiClient,
	private val context: Context,
	private val trickPlayEnabled: Boolean,
	private val forwardTime: Long
) : PlaybackSeekDataProvider() {
	private val imageRequests = mutableMapOf<Int, Disposable>()

	private var cachedPlaceholderThumbnail: Bitmap? = null

	override fun getSeekPositions(): LongArray {
		if (!videoPlayerAdapter.canSeek()) return LongArray(0)

		val duration = videoPlayerAdapter.duration
		val size = ceil(duration.toDouble() / forwardTime.toDouble()).toInt() + 1
		return LongArray(size) { i -> min(i * forwardTime, duration) }
	}

	override fun getThumbnail(index: Int, callback: ResultCallback) {
		if (!trickPlayEnabled) return

		val currentRequest = imageRequests[index]
		if (currentRequest?.isDisposed == false) currentRequest.dispose()

		val item = videoPlayerAdapter.currentlyPlayingItem
		val mediaSource = videoPlayerAdapter.currentMediaSource
		val mediaSourceId = mediaSource?.id?.toUUIDOrNull()
		if (item == null || mediaSource == null || mediaSourceId == null) return

		val trickPlayResolutions = item.trickplay?.get(mediaSource.id)
		val trickPlayInfo = trickPlayResolutions?.values?.firstOrNull()
		if (trickPlayInfo == null) return

		val currentTimeMs = (index * forwardTime).coerceIn(0, videoPlayerAdapter.duration)
		val currentTile = currentTimeMs.floorDiv(trickPlayInfo.interval).toInt()

		val tileSize = trickPlayInfo.tileWidth * trickPlayInfo.tileHeight
		val tileOffset = currentTile % tileSize
		val tileIndex = currentTile / tileSize

		val tileOffsetX = tileOffset % trickPlayInfo.tileWidth
		val tileOffsetY = tileOffset / trickPlayInfo.tileWidth
		val offsetX = tileOffsetX * trickPlayInfo.width
		val offsetY = tileOffsetY * trickPlayInfo.height

		val url = api.trickplayApi.getTrickplayTileImageUrl(
			itemId = item.id,
			width = trickPlayInfo.width,
			index = tileIndex,
			mediaSourceId = mediaSourceId,
		)

		val placeholderThumbnail = getPlaceholderThumbnail(trickPlayInfo.width, trickPlayInfo.height)

		imageRequests[index] = imageLoader.enqueue(ImageRequest.Builder(context).apply {
			data(url)
			size(Size.ORIGINAL)
			maxBitmapSize(Size(Dimension.Undefined, Dimension.Undefined))
			httpHeaders(NetworkHeaders.Builder().apply {
				set(
					key = "Authorization",
					value = AuthorizationHeaderBuilder.buildHeader(
						api.clientInfo.name,
						api.clientInfo.version,
						api.deviceInfo.id,
						api.deviceInfo.name,
						api.accessToken
					)
				)
			}.build())

			transformations(SubsetTransformation(offsetX, offsetY, trickPlayInfo.width, trickPlayInfo.height))

			target(
				onStart = { _ -> callback.onThumbnailLoaded(placeholderThumbnail, index) },
				onError = { _ -> callback.onThumbnailLoaded(placeholderThumbnail, index) },
				onSuccess = { image ->
					val bitmap = image.toBitmap()
					callback.onThumbnailLoaded(bitmap, index)
				}
			)
		}.build())
	}

	override fun reset() {
		for (request in imageRequests.values) {
			if (!request.isDisposed) request.dispose()
		}
		imageRequests.clear()
	}

	fun getPlaceholderThumbnail(width: Int, height: Int): Bitmap {
		if (cachedPlaceholderThumbnail?.width == width && cachedPlaceholderThumbnail?.height == height) {
			return cachedPlaceholderThumbnail!!
		}

		val color = ContextCompat.getColor(context, R.color.black_transparent_light)
		val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		result.eraseColor(color)
		cachedPlaceholderThumbnail = result
		return result
	}
}
