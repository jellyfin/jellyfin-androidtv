package org.jellyfin.playback.media3.exoplayer.compat

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.VideoRendererEventListener
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [DefaultRenderersFactory] that injects [DvCompatVideoRenderer] to rewrite
 * Dolby Vision Profile 7 streams as Profile 8.1 before presenting to MediaCodec.
 */
@OptIn(UnstableApi::class)
class DvCompatRenderersFactory(
	context: Context,
	private val forceCompatMode: Boolean,
	private val dvP7Hint: AtomicBoolean,
) : DefaultRenderersFactory(context) {

	override fun buildVideoRenderers(
		context: Context,
		extensionRendererMode: Int,
		mediaCodecSelector: MediaCodecSelector,
		enableDecoderFallback: Boolean,
		eventHandler: Handler,
		eventListener: VideoRendererEventListener,
		allowedVideoJoiningTimeMs: Long,
		out: ArrayList<Renderer>,
	) {
		out.add(
			DvCompatVideoRenderer(
				context = context,
				codecAdapterFactory = MediaCodecAdapter.Factory.getDefault(context),
				mediaCodecSelector = mediaCodecSelector,
				allowedJoiningTimeMs = allowedVideoJoiningTimeMs,
				enableDecoderFallback = enableDecoderFallback,
				forceCompatMode = forceCompatMode,
				dvP7Hint = dvP7Hint,
				eventHandler = eventHandler,
				eventListener = eventListener,
			)
		)
	}
}
