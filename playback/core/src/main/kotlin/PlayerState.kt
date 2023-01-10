package org.jellyfin.playback.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.model.VideoSize
import org.jellyfin.playback.core.queue.EmptyQueue
import org.jellyfin.playback.core.queue.Queue
import org.jellyfin.playback.core.queue.item.QueueEntry
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface PlayerState {
	val queue: StateFlow<Queue>
	val currentEntry: StateFlow<QueueEntry?>
	val playState: StateFlow<PlayState>
	val speed: StateFlow<Float>
	val videoSize: StateFlow<VideoSize>

	// Not reactive for performance (position would update every millisecond)
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

class MutablePlayerstate : PlayerState {
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
		get() = _backend?.getPositionInfo() ?: PositionInfo.EMPTY

	private var _backend: PlayerBackend? = null

	fun setBackend(backend: PlayerBackend) {
		// Remove listener from current backend
		_backend?.setListener(null)
		// Start listening on new backend
		backend.setListener(BackendEventListener())
		// Store new backend
		_backend = backend
	}

	inner class BackendEventListener : PlayerBackendEventListener {
		// TODO $speed
		override fun onPlayStateChange(state: PlayState) {
			_playState.value = state
		}

		override fun onVideoSizeChange(width: Int, height: Int) {
			_videoSize.value = VideoSize(width, height)
		}
	}

	fun setCurrentEntry(currentEntry: QueueEntry?) {
		_currentEntry.value = currentEntry
	}

	override fun play(queue: Queue) {
		_queue.value = queue
	}

	override fun pause() {
		// TODO: enqueue action when backend is not set
		_backend?.pause()
	}

	override fun unpause() {
		_backend?.play()
	}

	override fun stop() {
		_backend?.stop()
		_queue.value = EmptyQueue
	}

	override fun seek(to: Duration) {
		_backend?.seekTo(to)
	}

	private fun seekRelative(amount: Duration) {
		val current = _backend?.getPositionInfo()?.active ?: Duration.ZERO
		_backend?.seekTo(current + amount)
	}

	override fun fastForward(amount: Duration?) {
		// TODO use user preference instead of hardcoded "10.seconds"
		seekRelative(amount ?: 10.seconds)
	}

	override fun rewind(amount: Duration?) {
		// TODO use user preference instead of hardcoded "10.seconds"
		seekRelative(-(amount ?: 10.seconds))
	}

	override fun setSpeed(speed: Float) {
		_speed.value = speed
		_backend?.setSpeed(speed)
	}
}
