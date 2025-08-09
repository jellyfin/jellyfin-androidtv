package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
import androidx.leanback.widget.PlaybackSeekDataProvider
import coil3.ImageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size
import coil3.toBitmap
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
	private val interval: Long
) : PlaybackSeekDataProvider() {
	private val imageRequests = mutableMapOf<Int, Disposable>()

	override fun getSeekPositions(): LongArray {
		if (!videoPlayerAdapter.canSeek()) return LongArray(0)

		val duration = videoPlayerAdapter.duration
		val size = ceil(duration.toDouble() / interval.toDouble()).toInt() + 1
		return LongArray(size) { i -> min(i * interval, duration) }
	}

	private fun getTileSheetData(timeMs: Long): Pair<String, NetworkHeaders>? {
		val item = videoPlayerAdapter.currentlyPlayingItem
		val mediaSource = videoPlayerAdapter.currentMediaSource
		val mediaSourceId = mediaSource?.id?.toUUIDOrNull()
		if (item == null || mediaSource == null || mediaSourceId == null) return null

		val trickPlayResolutions = item.trickplay?.get(mediaSource.id)
		val trickPlayInfo = trickPlayResolutions?.values?.firstOrNull()
		if (trickPlayInfo == null) return null

		val tileNumber = timeMs.floorDiv(trickPlayInfo.interval).toInt()
		val tilesPerSheet = trickPlayInfo.tileWidth * trickPlayInfo.tileHeight  
		val sheetIndex = tileNumber / tilesPerSheet

		val url = api.trickplayApi.getTrickplayTileImageUrl(
			itemId = item.id,
			width = trickPlayInfo.width,
			index = sheetIndex,
			mediaSourceId = mediaSourceId,
		)

		val headers = NetworkHeaders.Builder().apply {
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
		}.build()

		return Pair(url, headers)
	}

	override fun getThumbnail(index: Int, callback: ResultCallback) {
		if (!trickPlayEnabled) return

		val currentRequest = imageRequests[index]
		if (currentRequest?.isDisposed == false) currentRequest.dispose()

		val currentTimeMs = (index * interval).coerceIn(0, videoPlayerAdapter.duration)
		val (url, headers) = getTileSheetData(currentTimeMs) ?: return

		val item = videoPlayerAdapter.currentlyPlayingItem
		val mediaSource = videoPlayerAdapter.currentMediaSource
		val trickPlayInfo = item?.trickplay?.get(mediaSource?.id)?.values?.firstOrNull() ?: return

		val currentTile = currentTimeMs.floorDiv(trickPlayInfo.interval).toInt()
		val tileSize = trickPlayInfo.tileWidth * trickPlayInfo.tileHeight
		val tileOffset = currentTile % tileSize

		val tileOffsetX = tileOffset % trickPlayInfo.tileWidth
		val tileOffsetY = tileOffset / trickPlayInfo.tileWidth
		val offsetX = tileOffsetX * trickPlayInfo.width
		val offsetY = tileOffsetY * trickPlayInfo.height

		imageRequests[index] = imageLoader.enqueue(ImageRequest.Builder(context).apply {
			data(url)
			size(Size.ORIGINAL)
			httpHeaders(headers)

			transformations(SubsetTransformation(offsetX, offsetY, trickPlayInfo.width, trickPlayInfo.height))

			target(
				onStart = { _ -> callback.onThumbnailLoaded(null, index) },
				onError = { _ -> callback.onThumbnailLoaded(null, index) },
				onSuccess = { image ->
					val bitmap = image.toBitmap()
					callback.onThumbnailLoaded(bitmap, index)
				}
			)
		}.build())
	}

	fun prefetchTileSheet(timeMs: Long) {
		if (!trickPlayEnabled) return

		val (url, headers) = getTileSheetData(timeMs) ?: return

		imageLoader.enqueue(ImageRequest.Builder(context).apply {
			data(url)
			httpHeaders(headers)
		}.build())
	}

	override fun reset() {
		for (request in imageRequests.values) {
			if (!request.isDisposed) request.dispose()
		}
		imageRequests.clear()
	}
}
