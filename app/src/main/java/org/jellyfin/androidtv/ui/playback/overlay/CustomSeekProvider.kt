package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
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
import org.jellyfin.androidtv.util.coil.SubsetTransformation
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.trickplayApi
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

class CustomSeekProvider(
	private val videoPlayerAdapter: VideoPlayerAdapter,
	private val imageLoader: ImageLoader,
	private val api: ApiClient,
	private val context: Context,
	private val trickPlayEnabled: Boolean,
	private val forwardTime: Long
) : PlaybackSeekDataProvider() {
	private val imageRequests = mutableMapOf<Int, Disposable>()

	override fun getSeekPositions(): LongArray {
		if (!videoPlayerAdapter.canSeek()) return LongArray(0)

		// Intentionally reduce resolution from milliseconds to seconds to avoid seeking less than a second which would confuse users
		val currentPositionSeconds = videoPlayerAdapter.currentPosition / 1000
		val forwardTimeSeconds = forwardTime / 1000
		val firstSeekPositionSeconds = currentPositionSeconds % forwardTimeSeconds
		val videoEndPositionSeconds = videoPlayerAdapter.duration / 1000

		val seekPositionCount = ((videoEndPositionSeconds - firstSeekPositionSeconds) / forwardTimeSeconds).toInt() + 1

		val seekPositions = ArrayList<Long>(seekPositionCount + 2)
		// Omit explicitly adding the beginning of the video as a seek position if the first seek position already represents it
		if (firstSeekPositionSeconds != 0L) {
			seekPositions.add(0L)
		}
		// Add all available seek positions but the last one
		for (i in 0..<seekPositionCount - 1) {
			seekPositions.add((firstSeekPositionSeconds + (i * forwardTimeSeconds)) * 1000)
		}
		// Omit explicitly adding the last seek position if the video end already represents it to avoid a sub-second seek at the end
		val lastSeekPositionSeconds = firstSeekPositionSeconds + ((seekPositionCount - 1) * forwardTimeSeconds)
		if (lastSeekPositionSeconds != videoEndPositionSeconds) {
			seekPositions.add(lastSeekPositionSeconds * 1000)
		}
		seekPositions.add(videoPlayerAdapter.duration)

		return seekPositions.toLongArray()
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
				onStart = { _ -> callback.onThumbnailLoaded(null, index) },
				onError = { _ -> callback.onThumbnailLoaded(null, index) },
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
}
