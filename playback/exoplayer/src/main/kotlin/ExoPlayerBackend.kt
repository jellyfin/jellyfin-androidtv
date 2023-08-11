package org.jellyfin.playback.exoplayer

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import org.jellyfin.playback.core.backend.BasePlayerBackend
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PositionInfo
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

class ExoPlayerBackend(
	private val context: Context,
) : BasePlayerBackend() {
	private var currentStream: MediaStream? = null

	private val exoPlayer by lazy {
		val renderersFactory = DefaultRenderersFactory(context).apply {
			setEnableDecoderFallback(true)
			setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
		}

		ExoPlayer.Builder(context, renderersFactory)
			.setRenderersFactory(DefaultRenderersFactory(context).apply {
				setEnableDecoderFallback(true)
				setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
			})
			.setPauseAtEndOfMediaItems(true)
			.build()
			.also { player -> player.addListener(PlayerListener()) }
	}

	inner class PlayerListener : Player.Listener {
		override fun onIsPlayingChanged(isPlaying: Boolean) {
			val state = when {
				isPlaying -> PlayState.PLAYING
				exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playbackState == Player.STATE_ENDED -> PlayState.STOPPED
				else -> PlayState.PAUSED
			}
			listener?.onPlayStateChange(state)
		}

		override fun onPlayerError(error: PlaybackException) {
			listener?.onPlayStateChange(PlayState.ERROR)
		}

		override fun onVideoSizeChanged(size: VideoSize) {
			listener?.onVideoSizeChange(size.width, size.height)
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			onIsPlayingChanged(exoPlayer.isPlaying)
		}

		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
			if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
				listener?.onMediaStreamEnd(requireNotNull(currentStream))
			}
		}
	}

	override fun prepareStream(stream: MediaStream) {
		val mediaItem = MediaItem.Builder().apply {
			setTag(stream)
			setMediaId(stream.hashCode().toString())
			setUri(stream.url)
		}.build()

		// Remove any old preloaded items (skips the first which is the playing item)
		while (exoPlayer.mediaItemCount > 1) exoPlayer.removeMediaItem(0)
		// Add new item
		exoPlayer.addMediaItem(mediaItem)

		exoPlayer.prepare()
	}

	override fun playStream(stream: MediaStream) {
		if (currentStream == stream) return

		currentStream = stream

		var streamIsPrepared = false
		repeat(exoPlayer.mediaItemCount) { index ->
			streamIsPrepared = streamIsPrepared || exoPlayer.getMediaItemAt(index).mediaId == stream.hashCode().toString()
		}

		if (!streamIsPrepared) prepareStream(stream)

		exoPlayer.seekToNextMediaItem()
		exoPlayer.play()
	}

	override fun play() {
		exoPlayer.play()
	}

	override fun pause() {
		exoPlayer.pause()
	}

	override fun stop() {
		exoPlayer.stop()
		currentStream = null
	}

	override fun seekTo(position: Duration) {
		if (!exoPlayer.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
			Timber.w("Trying to seek but ExoPlayer doesn't support it for the current item")
		}

		exoPlayer.seekTo(position.inWholeMilliseconds)
	}

	override fun setSpeed(speed: Float) {
		if (!exoPlayer.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) {
			Timber.w("Trying to change speed but ExoPlayer doesn't support it for the current item")
		}

		exoPlayer.setPlaybackSpeed(speed)
	}

	override fun getPositionInfo(): PositionInfo = PositionInfo(
		active = exoPlayer.currentPosition.milliseconds,
		buffer = exoPlayer.bufferedPosition.milliseconds,
		duration = if (exoPlayer.duration == C.TIME_UNSET) ZERO else exoPlayer.duration.milliseconds,
	)
}
