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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

class ExoPlayerBackend(
	private val context: Context,
) : BasePlayerBackend() {
	override fun getPositionInfo(): PositionInfo = PositionInfo(
		active = exoPlayer.currentPosition.milliseconds,
		buffer = exoPlayer.bufferedPosition.milliseconds,
		duration = if (exoPlayer.duration == C.TIME_UNSET) ZERO else exoPlayer.duration.milliseconds,
	)

	private val exoPlayer by lazy {
		val renderersFactory = DefaultRenderersFactory(context).apply {
			setEnableDecoderFallback(true)
			setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
		}
		ExoPlayer.Builder(context, renderersFactory)
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
	}

	override fun prepareStream(stream: MediaStream) {
		val mediaItem = MediaItem.Builder().apply {
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
		var streamIsPrepared = false
		repeat(exoPlayer.mediaItemCount) { index ->
			streamIsPrepared = streamIsPrepared || exoPlayer.getMediaItemAt(index).mediaId == stream.hashCode().toString()
		}

		if (!streamIsPrepared) prepareStream(stream)

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
	}

	override fun seekTo(position: Duration) {
		exoPlayer.seekTo(position.inWholeMilliseconds)
	}

	override fun setSpeed(speed: Float) {
		exoPlayer.setPlaybackSpeed(speed)
	}
}
