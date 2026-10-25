package org.jellyfin.androidtv.ui.playback

import android.os.Handler
import android.text.TextUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.jellyfin.androidtv.di.KoinInitializer
import org.jellyfin.androidtv.di.defaultDeviceInfo
import org.jellyfin.androidtv.util.profile.createDeviceProfile
import org.jellyfin.playback.media3.exoplayer.mapping.getFfmpegAudioMimeType
import org.jellyfin.playback.media3.exoplayer.mapping.getFfmpegSubtitleMimeType
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.RawResponse
import org.jellyfin.sdk.api.client.util.ApiSerializer
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSegmentDtoQueryResult
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.PlaybackInfoResponse
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.deviceprofile.buildDeviceProfile
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.net.HttpURLConnection.HTTP_OK
import java.util.IdentityHashMap
import kotlin.time.Duration.Companion.seconds

@kotlin.OptIn(ExperimentalCoroutinesApi::class)
internal class PlaybackFixture(private val method: PlayMethod = PlayMethod.DIRECT_PLAY) {
	private val streams = IdentityHashMap<MediaStream, MediaStream>()
	private val queue get() = GlobalContext.get().get<VideoQueueManager>()

	private val fragment = mockk<CustomPlaybackOverlayFragment>(relaxed = true)
	private val manager = mockk<VideoManager>(relaxed = true)
	private lateinit var lifecycle: LifecycleRegistry
	private lateinit var controller: PlaybackController

	fun initialize() {
		initializeAndroidEnvironment()
		initializeKoin()
		initializeFragment()
		initializeManager()

		controller = PlaybackController(emptyList(), fragment)
		controller.init(manager, fragment)
	}

	private fun initializeAndroidEnvironment() {
		Dispatchers.setMain(Dispatchers.Unconfined)
		mockkStatic("org.jellyfin.androidtv.util.profile.DeviceProfileKt")
		mockkStatic(TextUtils::class)
		mockkConstructor(Handler::class)

		every { createDeviceProfile(any(), any(), any()) } returns buildDeviceProfile {}
		every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
		every { anyConstructed<Handler>().postDelayed(any<Runnable>(), any<Long>()) } returns true
		justRun { anyConstructed<Handler>().removeCallbacks(any()) }
	}

	private fun initializeKoin() {
		val koinApplication = KoinInitializer().create(mockk(relaxed = true)).modules(module {
			single(defaultDeviceInfo) { DeviceInfo("id", "name") }
			single { spyk(get<Jellyfin>().createApi(baseUrl = "https://example.com")) }
		})

		coEvery { koinApplication.koin.get<ApiClient>().request(any(), any(), any(), any(), any()) } answers {
			val body = checkNotNull(ApiSerializer.encodeRequestBody(responseBody(arg(4))))
			RawResponse(body.encodeToByteArray(), HTTP_OK, emptyMap())
		}
	}

	private fun responseBody(request: Any?): Any {
		return if (request is PlaybackInfoDto) PlaybackInfoResponse(
			mediaSources = controller.currentlyPlayingItem.mediaSources.orEmpty().map { source ->
				source.copy(
					defaultAudioStreamIndex = request.audioStreamIndex ?: source.defaultAudioStreamIndex,
					defaultSubtitleStreamIndex = request.subtitleStreamIndex ?: source.defaultSubtitleStreamIndex,
				)
			}
		) else MediaSegmentDtoQueryResult(
			items = emptyList(), totalRecordCount = 0, startIndex = 0
		)
	}

	private fun initializeFragment() {
		fragment.leanbackOverlayFragment = mockk(relaxed = true)
		lifecycle = LifecycleRegistry.createUnsafe(fragment)
		lifecycle.currentState = Lifecycle.State.STARTED
		every { fragment.lifecycle } returns lifecycle
	}

	@androidx.annotation.OptIn(UnstableApi::class)
	private fun initializeManager() {
		manager.mExoPlayer = mockk(relaxed = true)
		every { manager.isInitialized } returns true
		every { manager.isPlaying } returns true

		every { manager.getExoPlayerTrack(any(), any()) } answers { callOriginal() }
		every { manager.setExoPlayerTrack(any(), any(), any()) } answers { callOriginal() }
		every { manager.mExoPlayer.currentTracks } answers {
			Tracks(
				controller.currentMediaSource.mediaStreams.orEmpty()
					.filter { it.type == MediaStreamType.AUDIO || it.type == MediaStreamType.SUBTITLE }
					.map(::toTracksGroup))
		}

		every {
			manager["offsetStreamIndex"](any<Int>(), any<Boolean>(), any<List<MediaStream>>())
		} answers { callOriginal() }

	}

	@androidx.annotation.OptIn(UnstableApi::class)
	private fun toTracksGroup(stream: MediaStream): Tracks.Group {
		val isSubtitle = stream.type == MediaStreamType.SUBTITLE
		val source = controller.currentMediaSource
		val codec = requireNotNull(stream.codec)
		val index = stream.index.toString()

		val id = if (stream.deliveryMethod == SubtitleDeliveryMethod.EXTERNAL) "1:JF_EXTERNAL:${index}" else index
		val mimeType = if (isSubtitle) getFfmpegSubtitleMimeType(codec) else getFfmpegAudioMimeType(codec)
		val defaultStream = if (isSubtitle) source.defaultSubtitleStreamIndex else source.defaultAudioStreamIndex

		return Tracks.Group(
			TrackGroup(id, Format.Builder().setId(id).setSampleMimeType(mimeType).build()),
			false,
			intArrayOf(C.FORMAT_HANDLED),
			booleanArrayOf(stream.index == defaultStream)
		)
	}

	private fun indexStreams(item: BaseItemDto): BaseItemDto {
		val source = item.mediaSources.orEmpty().single()
		val indexed = source.mediaStreams.orEmpty().sortedBy { it.isExternal }.mapIndexed { index, stream ->
			stream.copy(index = index).also { check(streams.putIfAbsent(stream, it) == null) }
		}

		val firstAudio = indexed.first { it.type == MediaStreamType.AUDIO }
		val defaultAudio = indexed.firstOrNull { it.type == MediaStreamType.AUDIO && it.isDefault }
		val defaultSubtitle = indexed.firstOrNull { it.type == MediaStreamType.SUBTITLE && it.isDefault }

		val processed = source.copy(
			mediaStreams = indexed,
			defaultAudioStreamIndex = (defaultAudio ?: firstAudio).index,
			defaultSubtitleStreamIndex = defaultSubtitle?.index ?: -1,
			supportsDirectPlay = method == PlayMethod.DIRECT_PLAY,
			supportsDirectStream = method == PlayMethod.DIRECT_STREAM,
			transcodingUrl = "/test.m3u8",
		)
		return item.copy(mediaStreams = indexed, mediaSources = listOf(processed))
	}

	fun getAudioIndex(): Int = controller.audioStreamIndex

	fun getSubtitleIndex(): Int = controller.subtitleStreamIndex

	fun getStreamIndex(stream: MediaStream?): Int = stream?.let { streams.getValue(it).index } ?: -1

	fun isCurrentStream(stream: MediaStream?): Boolean =
		stream == null || controller.currentlyPlayingItem.mediaStreams.orEmpty().any { it === streams.getValue(stream) }

	fun isAudioStream(stream: MediaStream): Boolean =
		isCurrentStream(stream) && getStreamIndex(stream) == getAudioIndex()

	fun isSubtitleStream(stream: MediaStream?): Boolean =
		isCurrentStream(stream) && getStreamIndex(stream) == getSubtitleIndex()

	fun assertAudio(stream: MediaStream) {
		isCurrentStream(stream) shouldBe true
		getAudioIndex() shouldBe getStreamIndex(stream)
	}

	fun assertSubtitle(stream: MediaStream?) {
		isCurrentStream(stream) shouldBe true
		getSubtitleIndex() shouldBe getStreamIndex(stream)
	}

	suspend fun play(vararg items: BaseItemDto) {
		setupPlayback {
			controller.stop()
			streams.clear()
			queue.setCurrentVideoQueue(items.map { indexStreams(it) })
			startController()
		}
	}

	suspend fun refresh() = setupPlayback { controller.refreshStream() }

	suspend fun skip() = setupPlayback { controller.next() }

	suspend fun next() {
		if (!controller.hasNextItem()) {
			controller.onCompletion()
			return
		}

		setupPlayback {
			controller.onCompletion()
			if (!controller.hasFragment()) {
				startController()
			}
		}
	}

	suspend fun selectAudio(stream: MediaStream) =
		setupPlayback { controller.switchAudioStream(getStreamIndex(stream)) }

	suspend fun selectSubtitle(stream: MediaStream?) =
		setupPlayback { controller.setSubtitleIndex(getStreamIndex(stream)) }

	suspend fun disableSubtitles() = selectSubtitle(null)

	private fun startController() {
		controller = PlaybackController(queue.getCurrentVideoQueue(), fragment, queue.getCurrentMediaPosition())
		controller.init(manager, fragment)
		controller.play(0)
	}

	private suspend fun setupPlayback(command: () -> Unit) {
		val started = CompletableDeferred<Unit>()
		every { manager.start() } answers { started.complete(Unit) }
		command()

		if (!controller.isPlaying) {
			withTimeout(5.seconds) { started.await() }
			controller.onPrepared()
		}

		check(controller.isPlaying)
	}

	fun deinitialize() {
		try {
			if (::controller.isInitialized) {
				controller.endPlayback()
			}

			if (::lifecycle.isInitialized && lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
				lifecycle.currentState = Lifecycle.State.DESTROYED
			}
		} finally {
			unmockkConstructor(Handler::class)
			unmockkStatic(TextUtils::class)
			unmockkStatic("org.jellyfin.androidtv.util.profile.DeviceProfileKt")
			stopKoin()
			Dispatchers.resetMain()
		}
	}
}
