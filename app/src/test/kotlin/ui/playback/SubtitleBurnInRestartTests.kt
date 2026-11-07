package org.jellyfin.androidtv.ui.playback

import androidx.lifecycle.Lifecycle
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.Job
import org.jellyfin.androidtv.data.compat.StreamInfo
import org.jellyfin.androidtv.data.compat.VideoOptions
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod

class SubtitleBurnInRestartTests : FunSpec({
	for (subtitleIndex in listOf(null, 7)) {
		test("playback response restores burn-in state with requested subtitle index $subtitleIndex") {
			val controller = spyk(mockk<PlaybackController>(), recordPrivateCalls = true)
			val manager = mockk<PlaybackManager>()
			val fragment = mockk<CustomPlaybackOverlayFragment>()
			val item = mockk<BaseItemDto>()
			val options = VideoOptions().apply {
				audioStreamIndex = 2
				subtitleStreamIndex = subtitleIndex
			}
			val stream = mockk<StreamInfo> {
				every { playMethod } returns PlayMethod.TRANSCODE
				every { subtitleDeliveryMethod } returns SubtitleDeliveryMethod.ENCODE
			}
			val callback = slot<Response<StreamInfo>>()
			every { fragment.lifecycle.currentState } returns Lifecycle.State.STARTED
			every { manager.getVideoStreamInfo(fragment, options, 0L, capture(callback)) } returns Job()
			every { controller["startItem"](item, 0L, stream) } returns Unit

			fun field(name: String) = PlaybackController::class.java.getDeclaredField(name).apply {
				isAccessible = true
			}
			field("playbackManager").set(controller, lazyOf(manager))
			field("mFragment").set(controller, fragment)
			field("mVideoManager").set(controller, mockk<VideoManager>())
			// stop() clears this flag before the selected audio stream is requested again.
			field("burningSubs").setBoolean(controller, false)
			PlaybackController::class.java.getDeclaredMethod(
				"playInternal", BaseItemDto::class.java, Long::class.javaObjectType, VideoOptions::class.java,
			).apply { isAccessible = true }.invoke(controller, item, 0L, options)

			callback.captured.onResponse(stream)

			field("burningSubs").getBoolean(controller) shouldBe true
		}
	}
})
