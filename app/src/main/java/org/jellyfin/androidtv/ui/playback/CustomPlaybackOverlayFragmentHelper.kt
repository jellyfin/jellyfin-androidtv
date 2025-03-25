package org.jellyfin.androidtv.ui.playback

import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.ui.GuideChannelHeader
import org.jellyfin.androidtv.ui.asTimerInfoDto
import org.jellyfin.androidtv.ui.livetv.TvManager
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration

fun CustomPlaybackOverlayFragment.toggleFavorite() {
	val header = mSelectedProgramView as? GuideChannelHeader
	val channel = header?.channel ?: return

	val itemMutationRepository by inject<ItemMutationRepository>()
	val dataRefreshService by inject<DataRefreshService>()

	lifecycleScope.launch {
		runCatching {
			val userData = itemMutationRepository.setFavorite(
				item = header.channel.id,
				favorite = !(channel.userData?.isFavorite ?: false)
			)

			header.channel = header.channel.copy(userData = userData)
			header.findViewById<View>(R.id.favImage).isVisible = userData.isFavorite
			dataRefreshService.lastFavoriteUpdate = Instant.now()
		}
	}
}

fun CustomPlaybackOverlayFragment.refreshSelectedProgram() {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			val item = withContext(Dispatchers.IO) {
				api.userLibraryApi.getItem(mSelectedProgram.id).content
			}
			mSelectedProgram = item
		}.onFailure { error ->
			Timber.e(error, "Unable to get program details")
		}

		detailUpdateInternal();
	}
}

fun CustomPlaybackOverlayFragment.playChannel(id: UUID) {
	val api by inject<ApiClient>()
	val playbackControllerContainer by inject<PlaybackControllerContainer>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.userLibraryApi.getItem(id).content
			}
		}.fold(
			onSuccess = { channel ->
				playbackControllerContainer.playbackController?.setItems(listOf(channel))
				playbackControllerContainer.playbackController?.play(0)
			},
			onFailure = {
				Toast.makeText(
					requireContext(),
					getString(R.string.msg_video_playback_error),
					Toast.LENGTH_LONG
				).show()

				closePlayer()
			},
		)
	}
}

fun CustomPlaybackOverlayFragment.cancelTimer(id: String) {
	val api by inject<ApiClient>()
	val playbackControllerContainer by inject<PlaybackControllerContainer>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.cancelTimer(id)
			}
		}.fold(
			onSuccess = {
				Toast.makeText(
					requireContext(),
					getString(R.string.msg_recording_cancelled),
					Toast.LENGTH_LONG
				).show()
				playbackControllerContainer.playbackController?.updateTvProgramInfo()
				TvManager.forceReload()
			},
			onFailure = {
				Toast.makeText(
					requireContext(),
					getString(R.string.msg_unable_to_cancel),
					Toast.LENGTH_LONG
				).show()

				closePlayer()
			},
		)
	}
}

fun CustomPlaybackOverlayFragment.cancelSeriesTimer(id: String) {
	val api by inject<ApiClient>()
	val playbackControllerContainer by inject<PlaybackControllerContainer>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.cancelSeriesTimer(id)
			}
		}.fold(
			onSuccess = {
				Toast.makeText(
					requireContext(),
					getString(R.string.msg_recording_cancelled),
					Toast.LENGTH_LONG
				).show()
				playbackControllerContainer.playbackController?.updateTvProgramInfo()
				TvManager.forceReload()
			},
			onFailure = {
				Toast.makeText(
					requireContext(),
					getString(R.string.msg_unable_to_cancel),
					Toast.LENGTH_LONG
				).show()

				closePlayer()
			},
		)
	}
}

fun CustomPlaybackOverlayFragment.recordProgram(program: BaseItemDto, isSeries: Boolean) {
	val api by inject<ApiClient>()
	val playbackControllerContainer by inject<PlaybackControllerContainer>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				val defaultTimer by api.liveTvApi.getDefaultTimer()

				if (isSeries) {
					api.liveTvApi.createSeriesTimer(defaultTimer.copy(programId = program.id.toString()))
				} else {
					api.liveTvApi.createTimer(defaultTimer.asTimerInfoDto().copy(programId = program.id.toString()))
				}
			}
		}.fold(
			onSuccess = {
				Toast.makeText(
					requireContext(),
					getString(R.string.msg_set_to_record),
					Toast.LENGTH_LONG
				).show()
				playbackControllerContainer.playbackController?.updateTvProgramInfo()
				TvManager.forceReload()
			},
			onFailure = {
				Toast.makeText(
					requireContext(),
					getString(R.string.msg_unable_to_create_recording),
					Toast.LENGTH_LONG
				).show()
			}
		)
	}
}

fun CustomPlaybackOverlayFragment.askToSkip(position: Duration) {
	binding.skipOverlay.targetPosition = position
}
