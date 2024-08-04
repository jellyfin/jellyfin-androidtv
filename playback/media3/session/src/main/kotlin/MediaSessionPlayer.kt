package org.jellyfin.playback.media3.session

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player.Commands
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.model.RepeatMode
import org.jellyfin.playback.core.queue.metadata
import org.jellyfin.playback.core.queue.queue
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
internal class MediaSessionPlayer(
	looper: Looper,
	private val scope: CoroutineScope,
	private val state: org.jellyfin.playback.core.PlayerState,
	private val manager: PlaybackManager,
) : SimpleBasePlayer(looper) {
	init {
		// Invalidate mediasession state when certain player state changes
		manager.queue.entry.invalidateStateOnEach(scope)
		state.playState.invalidateStateOnEach(scope)
		state.videoSize.invalidateStateOnEach(scope)
		state.speed.invalidateStateOnEach(scope)
		state.playbackOrder.invalidateStateOnEach(scope)
	}

	// Little helper function for the init block
	private fun <T> StateFlow<T>.invalidateStateOnEach(
		scope: CoroutineScope,
	) = onEach {
		withContext(Dispatchers.Main) { invalidateState() }
	}.launchIn(scope)

	override fun getState(): State = State.Builder().apply {
		setAvailableCommands(Commands.Builder().apply {
			add(COMMAND_PLAY_PAUSE)
			add(COMMAND_PREPARE)
			add(COMMAND_STOP)
			add(COMMAND_SEEK_TO_DEFAULT_POSITION)
			add(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
			val allowPrevious = manager.queue.entryIndex.value > 0
			addIf(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, allowPrevious)
			addIf(COMMAND_SEEK_TO_PREVIOUS, allowPrevious)
			val allowNext = manager.queue.entryIndex.value < (manager.queue.estimatedSize - 1)
			addIf(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, allowNext)
			addIf(COMMAND_SEEK_TO_NEXT, allowNext)
			// add(COMMAND_SEEK_TO_MEDIA_ITEM)
			add(COMMAND_SEEK_BACK)
			add(COMMAND_SEEK_FORWARD)
			add(COMMAND_SET_SPEED_AND_PITCH)
			add(COMMAND_SET_SHUFFLE_MODE)
			add(COMMAND_SET_REPEAT_MODE)
			add(COMMAND_GET_CURRENT_MEDIA_ITEM)
			add(COMMAND_GET_TIMELINE)
			add(COMMAND_GET_MEDIA_ITEMS_METADATA)
			// add(COMMAND_SET_MEDIA_ITEMS_METADATA)
			// add(COMMAND_SET_MEDIA_ITEM)
			// add(COMMAND_CHANGE_MEDIA_ITEMS)
			// add(COMMAND_GET_AUDIO_ATTRIBUTES)
			// add(COMMAND_GET_VOLUME)
			add(COMMAND_GET_DEVICE_VOLUME)
			// add(COMMAND_SET_VOLUME)
			add(COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS)
			add(COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS)
			// add(COMMAND_SET_VIDEO_SURFACE)
			// add(COMMAND_GET_TEXT)
			// add(COMMAND_SET_TRACK_SELECTION_PARAMETERS)
			// add(COMMAND_GET_TRACKS)
		}.build())

		runBlocking {
			val current = manager.queue.entry.value

			if (current != null) {
				val previous = manager.queue.peekPrevious()
				val next = manager.queue.peekNext()

				val playlist = listOfNotNull(previous, current, next)
					.distinctBy { it.metadata.mediaId }
					.map {
						MediaItemData.Builder(requireNotNull(it.metadata.mediaId)).apply {
							setMediaItem(it.metadata.toMediaItem())
							setDurationUs(it.metadata.duration?.inWholeMicroseconds ?: C.TIME_UNSET)
						}.build()
					}
				setPlaylist(playlist)

				setPlaybackState(when (state.playState.value) {
					PlayState.STOPPED -> STATE_IDLE
					PlayState.PLAYING -> STATE_READY
					PlayState.PAUSED -> STATE_READY
					PlayState.ERROR -> STATE_ENDED
				})

				setCurrentMediaItemIndex(if (previous == null || playlist.size <= 1) 0 else 1)
			} else {
				setPlaybackState(STATE_IDLE)
				setCurrentMediaItemIndex(C.INDEX_UNSET)
			}
		}

		setContentPositionMs { state.positionInfo.active.inWholeMilliseconds }
		setContentBufferedPositionMs { state.positionInfo.buffer.inWholeMilliseconds }

		setPlayWhenReady(state.playState.value == PlayState.PLAYING, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
		setPlaybackParameters(PlaybackParameters(state.speed.value))
		setShuffleModeEnabled(state.playbackOrder.value != PlaybackOrder.DEFAULT)
		setRepeatMode(if (state.repeatMode.value == RepeatMode.NONE) REPEAT_MODE_OFF else REPEAT_MODE_ALL)
		setVideoSize(state.videoSize.value.let { VideoSize(it.width, it.height) })
		@Suppress("MagicNumber")
		setDeviceVolume((state.volume.volume * 100).toInt())
		setIsDeviceMuted(state.volume.muted)
		setSeekBackIncrementMs(manager.options.defaultRewindAmount().inWholeMilliseconds)
		setSeekForwardIncrementMs(manager.options.defaultFastForwardAmount().inWholeMilliseconds)
	}.build()

	override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
		Timber.d("handleSetPlayWhenReady(playWhenReady=${playWhenReady})")
		if (playWhenReady) state.unpause()
		else state.pause()
		return Futures.immediateVoidFuture()
	}

	override fun handlePrepare(): ListenableFuture<*> {
		Timber.d("handlePrepare()")
		return Futures.immediateVoidFuture()
	}

	override fun handleStop(): ListenableFuture<*> {
		Timber.d("handleStop()")
		state.stop()
		return Futures.immediateVoidFuture()
	}

	override fun handleSeek(
		mediaItemIndex: Int,
		positionMs: Long,
		seekCommand: Int,
	): ListenableFuture<*> = scope.future(Dispatchers.Main) {
		Timber.d("handleSeek(mediaItemIndex=$mediaItemIndex, positionMs=$positionMs, seekCommand=$seekCommand)")

		// Queue progress
		@Suppress("SwitchIntDef")
		when (seekCommand) {
			COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
			COMMAND_SEEK_TO_PREVIOUS -> manager.queue.previous()

			COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
			COMMAND_SEEK_TO_NEXT -> manager.queue.next()
		}

		// Seeking
		val to = when (positionMs) {
			C.TIME_UNSET -> Duration.ZERO
			else -> positionMs.milliseconds
		}
		state.seek(to)
	}

	override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
		Timber.d("handleSetPlaybackParameters(playbackParameters=${playbackParameters})")
		state.setSpeed(playbackParameters.speed)
		return Futures.immediateVoidFuture()
	}

	override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
		Timber.d("handleSetShuffleModeEnabled(shuffleModeEnabled=${shuffleModeEnabled})")
		val playbackOrder = when (shuffleModeEnabled) {
			true -> PlaybackOrder.SHUFFLE
			false -> PlaybackOrder.DEFAULT
		}
		state.setPlaybackOrder(playbackOrder)
		return Futures.immediateVoidFuture()
	}

	override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
		Timber.d("handleSetRepeatMode(repeatMode=${repeatMode})")
		val mode = when (repeatMode) {
			REPEAT_MODE_ONE,
			REPEAT_MODE_ALL -> RepeatMode.REPEAT_ENTRY_INFINITE

			else -> RepeatMode.NONE
		}
		state.setRepeatMode(mode)
		return Futures.immediateVoidFuture()
	}

	override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> {
		state.volume.increaseVolume()
		return Futures.immediateVoidFuture()
	}

	override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> {
		state.volume.decreaseVolume()
		return Futures.immediateVoidFuture()
	}

	override fun handleSetDeviceVolume(deviceVolume: Int, flags: Int): ListenableFuture<*> {
		@Suppress("MagicNumber")
		state.volume.setVolume(deviceVolume / 100f)
		return Futures.immediateVoidFuture()
	}

	override fun handleSetDeviceMuted(muted: Boolean, flags: Int): ListenableFuture<*> {
		if (muted) state.volume.mute()
		else state.volume.unmute()
		return Futures.immediateVoidFuture()
	}

	override fun handleRelease(): ListenableFuture<*> {
		return Futures.immediateVoidFuture()
	}
}
