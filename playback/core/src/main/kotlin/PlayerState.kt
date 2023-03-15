package org.jellyfin.playback.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.playback.core.backend.BackendService
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.model.VideoSize
import org.jellyfin.playback.core.queue.EmptyQueue
import org.jellyfin.playback.core.queue.Queue
import org.jellyfin.playback.core.queue.item.QueueEntry
import kotlin.time.Duration

interface PlayerState {
	val queue: StateFlow<Queue>
	val currentEntry: StateFlow<QueueEntry?>
	val playState: StateFlow<PlayState>
	val speed: StateFlow<Float>
	val videoSize: StateFlow<VideoSize>

	/**
	 * The position information for the currently playing item or [PositionInfo.EMPTY]. This
	 * property is not reactive to avoid performance penalties. Manually read the values every
	 * second for UI or read when neccesary.
	 */
	val positionInfo: PositionInfo

	// Queueing

	fun play(queue: Queue)
	fun stop()

	// Pausing

	fun pause()
	fun unpause()

	// Seeking

	fun seek(to: Duration)
	fun fastForward(amount: Duration? = null)
	fun rewind(amount: Duration? = null)

	// Playback properties (repeat & shuffle are managed by queue)

	fun setSpeed(speed: Float)
}

class MutablePlayerState(
	private val options: PlaybackManagerOptions,
	private val backendService: BackendService,
) : PlayerState {
	private val _queue = MutableStateFlow<Queue>(EmptyQueue)
	override val queue: StateFlow<Queue> get() = _queue.asStateFlow()

	private val _currentEntry = MutableStateFlow<QueueEntry?>(null)
	override val currentEntry: StateFlow<QueueEntry?> get() = _currentEntry.asStateFlow()

	private val _playState = MutableStateFlow(PlayState.STOPPED)
	override val playState: StateFlow<PlayState> get() = _playState.asStateFlow()

	private val _speed = MutableStateFlow(1f)
	override val speed: StateFlow<Float> get() = _speed.asStateFlow()

	private val _videoSize = MutableStateFlow(VideoSize.EMPTY)
	override val videoSize: StateFlow<VideoSize> get() = _videoSize.asStateFlow()

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

			override fun onMediaStreamEnd(mediaStream: MediaStream) = Unit
		})
	}

	fun setCurrentEntry(currentEntry: QueueEntry?) {
		_currentEntry.value = currentEntry
	}

	override fun play(queue: Queue) {
		_queue.value = queue
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
		_queue.value = EmptyQueue
	}

	override fun seek(to: Duration) {
		backendService.backend?.seekTo(to)
	}

	private fun seekRelative(amount: Duration) {
		val current = backendService.backend?.getPositionInfo()?.active ?: Duration.ZERO
		backendService.backend?.seekTo(current + amount)
	}

	override fun fastForward(amount: Duration?) {
		seekRelative(amount ?: options.defaultFastForwardAmount.value)
	}

	override fun rewind(amount: Duration?) {
		seekRelative(-(amount ?: options.defaultRewindAmount.value))
	}

	override fun setSpeed(speed: Float) {
		_speed.value = speed
		backendService.backend?.setSpeed(speed)
	}
}
