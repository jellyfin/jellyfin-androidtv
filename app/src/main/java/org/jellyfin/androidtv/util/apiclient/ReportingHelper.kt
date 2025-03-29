package org.jellyfin.androidtv.util.apiclient

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.compat.StreamInfo
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.PlaybackOrder
import org.jellyfin.sdk.model.api.PlaybackProgressInfo
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.api.RepeatMode
import timber.log.Timber
import java.time.Instant

class ReportingHelper(
	private val dataRefreshService: DataRefreshService,
	private val api: ApiClient,
) {
	fun reportStart(
		lifecycleOwner: LifecycleOwner,
		playbackController: PlaybackController?,
		item: BaseItemDto,
		streamInfo: StreamInfo,
		position: Long,
		paused: Boolean
	) {
		val info = PlaybackStartInfo(
			itemId = item.id,
			positionTicks = position,
			canSeek = (streamInfo.runTimeTicks ?: 0) > 0,
			isPaused = paused,
			liveStreamId = streamInfo.mediaSource?.liveStreamId,
			playSessionId = streamInfo.playSessionId,
			playMethod = streamInfo.playMethod,
			audioStreamIndex = playbackController?.audioStreamIndex,
			subtitleStreamIndex = playbackController?.subtitleStreamIndex,
			isMuted = false,
			repeatMode = RepeatMode.REPEAT_NONE,
			playbackOrder = PlaybackOrder.DEFAULT,
			mediaSourceId = streamInfo.mediaSourceId,
		)

		lifecycleOwner.lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
			Timber.i("Reporting ${item.name} playback started at $position")
			runCatching {
				api.playStateApi.reportPlaybackStart(info)
			}.onFailure { error -> Timber.e(error, "Failed to report started playback!") }
		}
	}

	fun reportProgress(
		lifecycleOwner: LifecycleOwner,
		playbackController: PlaybackController?,
		item: BaseItemDto,
		streamInfo: StreamInfo,
		position: Long,
		paused: Boolean
	) {
		val info = PlaybackProgressInfo(
			itemId = item.id,
			positionTicks = position,
			canSeek = (streamInfo.runTimeTicks ?: 0) > 0,
			isPaused = paused,
			liveStreamId = streamInfo.mediaSource?.liveStreamId,
			playSessionId = streamInfo.playSessionId,
			playMethod = streamInfo.playMethod,
			audioStreamIndex = playbackController?.audioStreamIndex,
			subtitleStreamIndex = playbackController?.subtitleStreamIndex,
			isMuted = false,
			repeatMode = RepeatMode.REPEAT_NONE,
			playbackOrder = PlaybackOrder.DEFAULT,
			mediaSourceId = streamInfo.mediaSourceId,
		)

		lifecycleOwner.lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
			Timber.d("Reporting ${item.name} playback progress at $position")
			runCatching {
				api.playStateApi.reportPlaybackProgress(info)
			}.onFailure { error -> Timber.w(error, "Failed to report playback progress") }
		}
	}

	fun reportStopped(lifecycleOwner: LifecycleOwner, item: BaseItemDto, streamInfo: StreamInfo, position: Long?) {
		val info = PlaybackStopInfo(
			itemId = item.id,
			positionTicks = position,
			mediaSourceId = streamInfo.mediaSourceId,
			liveStreamId = streamInfo.mediaSource?.liveStreamId,
			playSessionId = streamInfo.playSessionId,
			failed = false,
		)

		lifecycleOwner.lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
			Timber.i("Reporting ${item.name} playback stopped at $position")
			runCatching {
				api.playStateApi.reportPlaybackStopped(info)
			}.onFailure { error -> Timber.e(error, "Failed to report stopped playback!") }
		}

		// Update dataRefreshService
		dataRefreshService.lastPlayback = Instant.now()
		when (item.type) {
			BaseItemKind.MOVIE -> dataRefreshService.lastMoviePlayback = Instant.now()
			BaseItemKind.EPISODE -> dataRefreshService.lastTvPlayback = Instant.now()
			else -> Unit
		}
	}
}
