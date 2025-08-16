package org.jellyfin.playback.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.playback.core.backend.BackendService
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.model.RepeatMode
import org.jellyfin.playback.core.model.VideoSize
import org.jellyfin.playback.core.queue.QueueService
import kotlin.time.Duration

interface PlayerState {
	val volume: PlayerVolumeState
	val playState: StateFlow<PlayState>
	val speed: StateFlow<Float>
	val videoSize: StateFlow<VideoSize>
	val playbackOrder: StateFlow<PlaybackOrder>
	val repeatMode: StateFlow<RepeatMode>
	val scrubbing: StateFlow<Boolean>

	/**
	 * The position information for the currently playing item or [PositionInfo.EMPTY]. This
	 * property is not reactive to avoid performance penalties. Manually read the values every
	 * second for UI or read when necessary.
	 */
	val positionInfo: PositionInfo

	// Queue management
	fun play()
	fun stop()

	// Pausing

	fun pause()
	fun unpause()

	// Seeking

	fun seek(to: Duration)
	fun fastForward(amount: Duration? = null)
	fun rewind(amount: Duration? = null)
	fun setScrubbing(scrubbing: Boolean)

	// Playback properties

	fun setSpeed(speed: Float)

	fun setPlaybackOrder(order: PlaybackOrder)

	fun setRepeatMode(mode: RepeatMode)
}

class MutablePlayerState(
	private val options: PlaybackManagerOptions,
	private val backendService: BackendService,
	private val queue: QueueService?,
) : PlayerState {
	override val volume: PlayerVolumeState

	private val _playState = MutableStateFlow(PlayState.STOPPED)
	override val playState: StateFlow<PlayState> get() = _playState.asStateFlow()

	private val _speed = MutableStateFlow(1f)
	override val speed: StateFlow<Float> get() = _speed.asStateFlow()

	private val _videoSize = MutableStateFlow(VideoSize.EMPTY)
	override val videoSize: StateFlow<VideoSize> get() = _videoSize.asStateFlow()

	private val _playbackOrder = MutableStateFlow(PlaybackOrder.DEFAULT)
	override val playbackOrder: StateFlow<PlaybackOrder> get() = _playbackOrder.asStateFlow()

	private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
	override val repeatMode: StateFlow<RepeatMode> get() = _repeatMode.asStateFlow()

	private val _scrubbing = MutableStateFlow(false)
	override val scrubbing: StateFlow<Boolean> get() = _scrubbing.asStateFlow()

	override val positionInfo: PositionInfo
		get() = backendService.backend?.getPositionInfo() ?: PositionInfo.EMPTY

	init {
		backendService.addListener(object : PlayerBackendEventListener {
			override fun onPlayStateChange(state: PlayState) {
				_playState.value = state
			}

			override fun onVideoSizeChange(width: Int, height: Int) {
				_videoSize.value = VideoSize(width, height)
			}

			override fun onMediaStreamEnd(mediaStream: PlayableMediaStream) {
				// Make sure to start stream again if repeat mode is turned on
				// Note: the QueueService is responsible for changing REPEAT_ENTRY_ONCE to NONE
				if (_repeatMode.value != RepeatMode.NONE) {
					backendService.backend?.play()
				}
			}
		})

		volume = options.playerVolumeState
	}

	override fun play() {
		backendService.backend?.play()
	}

	override fun pause() {
		// TODO: enqueue action when backend is not set
		backendService.backend?.pause()
	}

	override fun unpause() {
		backendService.backend?.play()
	}

	override fun stop() {
		backendService.backend?.stop()
		queue?.clear()
	}

	override fun seek(to: Duration) {
		backendService.backend?.seekTo(to)
	}

	private fun seekRelative(amount: Duration) {
		val current = backendService.backend?.getPositionInfo()?.active ?: Duration.ZERO
		backendService.backend?.seekTo(current + amount)
	}

	override fun fastForward(amount: Duration?) {
		seekRelative(amount ?: options.defaultFastForwardAmount())
	}

	override fun rewind(amount: Duration?) {
		seekRelative(-(amount ?: options.defaultRewindAmount()))
	}

	override fun setScrubbing(scrubbing: Boolean) {
		_scrubbing.value = scrubbing
		backendService.backend?.setScrubbing(scrubbing)
	}

	override fun setSpeed(speed: Float) {
		_speed.value = speed
		backendService.backend?.setSpeed(speed)
	}

	override fun setPlaybackOrder(order: PlaybackOrder) {
		_playbackOrder.value = order
	}

	override fun setRepeatMode(mode: RepeatMode) {
		_repeatMode.value = mode
	}
}
