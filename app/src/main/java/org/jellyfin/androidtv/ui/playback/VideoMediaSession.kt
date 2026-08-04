package org.jellyfin.androidtv.ui.playback

import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.core.content.IntentCompat
import timber.log.Timber
import kotlin.math.abs

/**
 * Publishes a media session for the video player.
 *
 * Media button events coming from external devices - like a single click on a pair of Bluetooth headphones - are routed by the system to
 * the app owning an active media session and never reach the key handling in [CustomPlaybackOverlayFragment]. This class publishes such a
 * session and maps its transport controls onto the [PlaybackController].
 *
 * The published playback state is kept up to date because devices like AirPods use it to decide whether a click should send a play or a
 * pause command.
 */
class VideoMediaSession(
	context: Context,
	private val playbackControllerContainer: PlaybackControllerContainer,
) {
	companion object {
		private const val TAG = "JellyfinVideoPlayer"

		// Amount of milliseconds the position may drift before the session is updated again
		private const val POSITION_UPDATE_THRESHOLD = 5000L
	}

	private val playbackController get() = playbackControllerContainer.playbackController

	private var released = false
	private var lastState = PlaybackState.STATE_NONE
	private var lastPosition = -1L

	private val callback = object : MediaSession.Callback() {
		override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
			val keyEvent: KeyEvent? = IntentCompat.getParcelableExtra(mediaButtonIntent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
			val isToggleKey = keyEvent != null &&
				(keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyEvent.keyCode == KeyEvent.KEYCODE_HEADSETHOOK)

			// A single click on most wireless headphones sends a play/pause (or headset hook) key. Toggle playback based on our own state
			// instead of letting the default implementation decide based on the (possibly outdated) session state
			if (isToggleKey && keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
				Timber.i("Toggling playback for media button %d", keyEvent.keyCode)
				playbackController?.playPause()
				update()
				return true
			}

			return super.onMediaButtonEvent(mediaButtonIntent)
		}

		override fun onPlay() {
			val controller = playbackController ?: return
			if (controller.isPaused) controller.playPause()
			update()
		}

		override fun onPause() {
			val controller = playbackController ?: return
			if (!controller.isPaused) controller.pause()
			update()
		}

		override fun onStop() {
			playbackController?.endPlayback(true)
			update()
		}

		override fun onSkipToNext() {
			playbackController?.next()
		}

		override fun onSkipToPrevious() {
			playbackController?.prev()
		}

		override fun onFastForward() {
			playbackController?.fastForward()
		}

		override fun onRewind() {
			playbackController?.rewind()
		}

		override fun onSeekTo(pos: Long) {
			playbackController?.seek(pos)
			update()
		}
	}

	private val session = MediaSession(context, TAG).apply {
		// These flags are always set (and deprecated) since Android 8 but required for older versions
		@Suppress("DEPRECATION")
		setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
		setCallback(callback, Handler(Looper.getMainLooper()))
	}

	/**
	 * Update the metadata of the currently playing item.
	 */
	fun updateMetadata() {
		if (released) return

		val controller = playbackController
		val item = controller?.currentlyPlayingItem

		// Make sure the next state update is published as the available actions may have changed with the item
		lastState = PlaybackState.STATE_NONE

		session.setMetadata(MediaMetadata.Builder().apply {
			putString(MediaMetadata.METADATA_KEY_TITLE, item?.name.orEmpty())
			putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, item?.name.orEmpty())
			item?.seriesName?.let { seriesName -> putString(MediaMetadata.METADATA_KEY_ARTIST, seriesName) }
			putLong(MediaMetadata.METADATA_KEY_DURATION, controller?.duration ?: 0L)
		}.build())
	}

	/**
	 * Update the playback state and activate the session when playback is active. Safe to call for every progress update as unchanged
	 * states are ignored.
	 */
	fun update() {
		if (released) return

		val controller = playbackController
		val state = getState(controller)
		val position = controller?.currentPosition ?: 0L

		// Don't send an update for every progress tick
		if (state == lastState && abs(position - lastPosition) < POSITION_UPDATE_THRESHOLD) return
		lastState = state
		lastPosition = position

		val speed = when (state) {
			PlaybackState.STATE_PLAYING -> controller?.playbackSpeed?.takeIf { it > 0f } ?: 1f
			else -> 0f
		}

		session.setPlaybackState(
			PlaybackState.Builder()
				.setActions(getActions(controller))
				.setState(state, position, speed)
				.build()
		)

		// Only an active session receives media button events
		session.isActive = state != PlaybackState.STATE_NONE
	}

	private fun getState(controller: PlaybackController?) = when {
		controller == null -> PlaybackState.STATE_NONE
		controller.isPaused -> PlaybackState.STATE_PAUSED
		controller.isPlaying -> PlaybackState.STATE_PLAYING
		// Playback is starting, seeking or switching streams
		else -> PlaybackState.STATE_BUFFERING
	}

	private fun getActions(controller: PlaybackController?): Long {
		var actions = PlaybackState.ACTION_PLAY or
			PlaybackState.ACTION_PAUSE or
			PlaybackState.ACTION_PLAY_PAUSE or
			PlaybackState.ACTION_STOP or
			PlaybackState.ACTION_FAST_FORWARD or
			PlaybackState.ACTION_REWIND

		if (controller?.canSeek() == true) actions = actions or PlaybackState.ACTION_SEEK_TO
		if (controller?.hasNextItem() == true) actions = actions or PlaybackState.ACTION_SKIP_TO_NEXT
		if (controller?.hasPreviousItem() == true) actions = actions or PlaybackState.ACTION_SKIP_TO_PREVIOUS

		return actions
	}

	/**
	 * Deactivate and release the session. The instance cannot be used after calling this function.
	 */
	fun release() {
		if (released) return
		released = true

		session.isActive = false
		session.release()
	}
}
