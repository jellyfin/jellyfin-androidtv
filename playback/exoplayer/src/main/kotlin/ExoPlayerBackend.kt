package org.jellyfin.playback.exoplayer

import android.app.ActivityManager
import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.getSystemService
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.TsExtractor
import org.jellyfin.playback.core.backend.BasePlayerBackend
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.support.PlaySupportReport
import org.jellyfin.playback.exoplayer.support.getPlaySupportReport
import org.jellyfin.playback.exoplayer.support.toFormat
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
class ExoPlayerBackend(
	private val context: Context,
) : BasePlayerBackend() {
	companion object {
		const val TS_SEARCH_BYTES_LM = TsExtractor.TS_PACKET_SIZE * 1800
		const val TS_SEARCH_BYTES_HM = TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES
	}

	private var currentStream: PlayableMediaStream? = null

	private val exoPlayer by lazy {
		ExoPlayer.Builder(context)
			.setRenderersFactory(DefaultRenderersFactory(context).apply {
				setEnableDecoderFallback(true)
				setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
			})
			.setTrackSelector(DefaultTrackSelector(context).apply {
				setParameters(buildUponParameters().apply {
					setTunnelingEnabled(true)
					setAudioOffloadPreferences(TrackSelectionParameters.AudioOffloadPreferences.DEFAULT.buildUpon().apply {
						setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
					}.build())
				})
			})
			.setMediaSourceFactory(DefaultMediaSourceFactory(
				context,
				DefaultExtractorsFactory().apply {
					val isLowRamDevice = context.getSystemService<ActivityManager>()?.isLowRamDevice == true
					setTsExtractorTimestampSearchBytes(when (isLowRamDevice) {
						true -> TS_SEARCH_BYTES_LM
						false -> TS_SEARCH_BYTES_HM
					})
				}
			))
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

	override fun supportsStream(
		stream: MediaStream
	): PlaySupportReport = exoPlayer.getPlaySupportReport(stream.toFormat())

	override fun prepareStream(stream: PlayableMediaStream) {
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

	override fun playStream(stream: PlayableMediaStream) {
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
