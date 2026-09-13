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
	// Container alone is not sufficient: the server infers a target audio codec from the segment
	// request's own file extension when no explicit "audioCodec" is sent, which is wrong for an
	// HLS video segment (verified live: forcing segmentContainer=mp4 alone still produced AAC).
	// For fMP4 only the ts-incompatible subset of hlsFmp4AudioCodecs is sent (also verified live:
	// the full list trips the server's own 40-char limit on this parameter and gets rejected with
	// a 400) - that subset is also the precise reason fMP4 was negotiated in the first place.
	test("HLS source with fMP4 transcoding container requests that container and the codecs ts cannot carry") {
		val source = hlsMediaSource(transcodingContainer = "mp4")

		source.segmentContainerQueryParameters() shouldBe mapOf(
			"segmentContainer" to "mp4",
			"audioCodec" to "alac,flac,opus,dts,truehd",
		)
	}

	test("HLS source with the common MPEG-TS transcoding container requests ts and its declared codec list") {
		val source = hlsMediaSource(transcodingContainer = "ts")

		source.segmentContainerQueryParameters() shouldBe mapOf(
			"segmentContainer" to "ts",
			"audioCodec" to "aac,ac3,eac3,mp3",
		)
	}

	test("the audioCodec value for every recognized container stays within the server's 40-character limit") {
		val mp4Params = hlsMediaSource(transcodingContainer = "mp4").segmentContainerQueryParameters()
		val tsParams = hlsMediaSource(transcodingContainer = "ts").segmentContainerQueryParameters()

		((mp4Params["audioCodec"] as String).length <= 40) shouldBe true
		((tsParams["audioCodec"] as String).length <= 40) shouldBe true
	}

	test("HLS source with an unrecognized transcoding container still requests the container, without an audio codec list") {
		val source = hlsMediaSource(transcodingContainer = "mkv")

		source.segmentContainerQueryParameters() shouldBe mapOf("segmentContainer" to "mkv")
	}

	test("HLS source without a negotiated transcoding container adds no parameters (nothing to tell the server)") {
		val source = hlsMediaSource(transcodingContainer = null)

		source.segmentContainerQueryParameters().shouldBeEmpty()
	}

	test("non-HLS (progressive http) transcoding sub protocol is left untouched") {
		val source = hlsMediaSource(transcodingContainer = "mp4").copy(transcodingSubProtocol = MediaStreamProtocol.HTTP)

		source.segmentContainerQueryParameters().shouldBeEmpty()
	}
})
