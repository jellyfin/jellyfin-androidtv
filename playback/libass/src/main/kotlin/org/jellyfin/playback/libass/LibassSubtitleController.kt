package org.jellyfin.playback.libass

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.ui.SubtitleView
import io.github.peerless2012.ass.media.AssHandler
import io.github.peerless2012.ass.media.kt.withAssMkvSupport
import io.github.peerless2012.ass.media.kt.withAssSupport
import io.github.peerless2012.ass.media.parser.AssSubtitleParserFactory
import io.github.peerless2012.ass.media.type.AssRenderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.subtitleApi
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class LibassSubtitleController(
	private val renderType: AssRenderType = AssRenderType.OVERLAY_CANVAS,
) {
	private val assHandler = AssHandler(renderType)
	private var attachedPlayer: ExoPlayer? = null
	private var attachedSubtitleView: SubtitleView? = null

	fun configure(
		builder: ExoPlayer.Builder,
		dataSourceFactory: DataSource.Factory,
		extractorsFactory: ExtractorsFactory,
		renderersFactory: RenderersFactory,
	) {
		val subtitleParserFactory = AssSubtitleParserFactory(assHandler)
		val mediaSourceFactory = DefaultMediaSourceFactory(
			dataSourceFactory,
			extractorsFactory.withAssMkvSupport(subtitleParserFactory, assHandler),
		)

		mediaSourceFactory.setSubtitleParserFactory(subtitleParserFactory)

		builder
			.setMediaSourceFactory(mediaSourceFactory)
			.setRenderersFactory(renderersFactory.withAssSupport(assHandler))
	}

	fun attach(player: ExoPlayer, subtitleView: SubtitleView?) {
		if (attachedPlayer !== player) {
			assHandler.init(player)
			attachedPlayer = player
		}

		if (renderType == AssRenderType.OVERLAY_CANVAS || renderType == AssRenderType.OVERLAY_OPEN_GL) {
			if (subtitleView != null && attachedSubtitleView !== subtitleView) {
				subtitleView.withAssSupport(assHandler)
				attachedSubtitleView = subtitleView
			}
		}
	}

	fun clearSelectedTrack() {
		assHandler.render?.setTrack(null)
	}

	fun registerFallbackFonts(api: ApiClient) {
		val cacheKey = api.hashCode()
		val deferred = fallbackFontCache.computeIfAbsent(cacheKey) {
			fontScope.async {
				loadFallbackFonts(api)
			}
		}

		fontScope.launch {
			runCatching {
				deferred.await()
			}.onSuccess { fonts ->
				fonts.forEach { font ->
					assHandler.addFont(font.name, font.data)
				}
			}.onFailure { error ->
				fallbackFontCache.remove(cacheKey, deferred)
				Timber.w(error, "Unable to load Jellyfin fallback subtitle fonts for libass")
			}
		}
	}

	private suspend fun loadFallbackFonts(api: ApiClient): List<FallbackFont> {
		val fonts = api.subtitleApi.getFallbackFontList().content

		return fonts.mapNotNull { font ->
			val name = font.name ?: return@mapNotNull null
			runCatching {
				FallbackFont(name, api.subtitleApi.getFallbackFont(name).content)
			}.onFailure { error ->
				Timber.w(error, "Unable to load Jellyfin fallback subtitle font %s", name)
			}.getOrNull()
		}
	}

	private class FallbackFont(
		val name: String,
		val data: ByteArray,
	)

	companion object {
		private val fontScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
		private val fallbackFontCache = ConcurrentHashMap<Int, Deferred<List<FallbackFont>>>()
	}
}
