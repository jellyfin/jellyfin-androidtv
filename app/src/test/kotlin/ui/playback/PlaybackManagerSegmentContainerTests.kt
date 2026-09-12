package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaSourceType
import org.jellyfin.sdk.model.api.MediaStreamProtocol

// A DeviceProfile declares HLS transcoding support for multiple containers, split by audio
// codec (see deviceProfile.kt: MPEG-TS for AAC/AC3/EAC3, fMP4 for codecs such as TrueHD/DTS
// that MPEG-TS cannot carry). The server negotiates which one applies and reports it back as
// MediaSourceInfo.transcodingContainer, but historically does not apply that same choice to
// the HLS playlist/segment URL it hands back (MediaSourceInfo.transcodingUrl) unless an
// explicit "segmentContainer" query parameter is supplied - that endpoint otherwise falls back
// to its own ".ts" default regardless of what was negotiated. These tests pin down the exact
// query parameters PlaybackManager now derives from the negotiated MediaSourceInfo so an HLS
// playback session actually requests the container it was told to use.
private fun hlsMediaSource(transcodingContainer: String?) = MediaSourceInfo(
	protocol = MediaProtocol.HTTP,
	type = MediaSourceType.DEFAULT,
	isRemote = false,
	readAtNativeFramerate = false,
	ignoreDts = false,
	ignoreIndex = false,
	genPtsInput = false,
	supportsTranscoding = true,
	supportsDirectStream = false,
	supportsDirectPlay = false,
	isInfiniteStream = false,
	requiresOpening = false,
	requiresClosing = false,
	requiresLooping = false,
	supportsProbing = true,
	transcodingSubProtocol = MediaStreamProtocol.HLS,
	transcodingContainer = transcodingContainer,
	hasSegments = false,
)

class PlaybackManagerSegmentContainerTests : FunSpec({
	test("HLS source with fMP4 transcoding container (e.g. negotiated for TrueHD/DTS passthrough) requests that same segment container") {
		val source = hlsMediaSource(transcodingContainer = "mp4")

		source.segmentContainerQueryParameters() shouldBe mapOf("segmentContainer" to "mp4")
	}

	test("HLS source with the common MPEG-TS transcoding container still explicitly requests ts (keeps today's behavior)") {
		val source = hlsMediaSource(transcodingContainer = "ts")

		source.segmentContainerQueryParameters() shouldBe mapOf("segmentContainer" to "ts")
	}

	test("HLS source without a negotiated transcoding container adds no parameter (nothing to tell the server)") {
		val source = hlsMediaSource(transcodingContainer = null)

		source.segmentContainerQueryParameters().shouldBeEmpty()
	}

	test("non-HLS (progressive http) transcoding sub protocol is left untouched") {
		val source = hlsMediaSource(transcodingContainer = "mp4").copy(transcodingSubProtocol = MediaStreamProtocol.HTTP)

		source.segmentContainerQueryParameters().shouldBeEmpty()
	}
})
