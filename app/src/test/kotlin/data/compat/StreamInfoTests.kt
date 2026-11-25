package org.jellyfin.androidtv.data.compat

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod

private fun createMediaStream(
	index: Int,
	type: MediaStreamType,
	deliveryMethod: SubtitleDeliveryMethod? = null,
): MediaStream = mockk {
	every { this@mockk.index } returns index
	every { this@mockk.type } returns type
	every { this@mockk.deliveryMethod } returns deliveryMethod
}

private fun createStreamInfo(
	defaultSubtitleStreamIndex: Int?,
	mediaStreams: List<MediaStream>?,
): StreamInfo = StreamInfo().apply {
	mediaSource = mockk<MediaSourceInfo> {
		every { this@mockk.defaultSubtitleStreamIndex } returns defaultSubtitleStreamIndex
		every { this@mockk.mediaStreams } returns mediaStreams
	}
}

class StreamInfoTests : FunSpec({
	test("getSubtitleDeliveryMethod returns DROP when mediaSource is null") {
		val streamInfo = StreamInfo()

		streamInfo.subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.DROP
	}

	test("getSubtitleDeliveryMethod returns DROP when defaultSubtitleStreamIndex is null or negative") {
		val streams = listOf(createMediaStream(0, MediaStreamType.SUBTITLE, SubtitleDeliveryMethod.EMBED))

		createStreamInfo(null, streams).subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.DROP
		createStreamInfo(-1, streams).subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.DROP
		createStreamInfo(-5, streams).subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.DROP
	}

	test("getSubtitleDeliveryMethod returns DROP when mediaStreams is null or empty") {
		createStreamInfo(0, null).subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.DROP
		createStreamInfo(0, emptyList()).subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.DROP
	}

	test("getSubtitleDeliveryMethod finds subtitle stream by index when index exceeds mediaStreams size") {
		val streams = listOf(
			createMediaStream(0, MediaStreamType.AUDIO),
			createMediaStream(1, MediaStreamType.AUDIO),
			createMediaStream(2, MediaStreamType.VIDEO),
			createMediaStream(7, MediaStreamType.SUBTITLE, SubtitleDeliveryMethod.ENCODE),
			createMediaStream(8, MediaStreamType.EMBEDDED_IMAGE),
		)

		val streamInfo = createStreamInfo(7, streams)

		streamInfo.subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.ENCODE
	}

	test("getSubtitleDeliveryMethod matches stream index rather than list position") {
		val streams = listOf(
			createMediaStream(3, MediaStreamType.SUBTITLE, SubtitleDeliveryMethod.EXTERNAL),
			createMediaStream(0, MediaStreamType.SUBTITLE, SubtitleDeliveryMethod.EMBED),
		)

		createStreamInfo(0, streams).subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.EMBED
		createStreamInfo(3, streams).subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.EXTERNAL
	}

	test("getSubtitleDeliveryMethod returns DROP when index does not match any subtitle stream") {
		val streams = listOf(
			createMediaStream(0, MediaStreamType.VIDEO),
			createMediaStream(1, MediaStreamType.AUDIO),
			createMediaStream(2, MediaStreamType.SUBTITLE, SubtitleDeliveryMethod.EXTERNAL),
		)

		createStreamInfo(1, streams).subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.DROP
		createStreamInfo(99, streams).subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.DROP
	}

	test("getSubtitleDeliveryMethod returns DROP when matched subtitle stream has null deliveryMethod") {
		val streams = listOf(
			createMediaStream(2, MediaStreamType.SUBTITLE, null),
		)

		createStreamInfo(2, streams).subtitleDeliveryMethod shouldBe SubtitleDeliveryMethod.DROP
	}
})
