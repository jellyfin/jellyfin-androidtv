package org.jellyfin.androidtv.data.compat

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import java.util.UUID

class VideoOptionsTests : FunSpec({
	val streams = listOf(
		audioTestStream(index = 0, type = MediaStreamType.SUBTITLE, language = "rus"),
		audioTestStream(index = 1, type = MediaStreamType.AUDIO, language = "rus"),
		audioTestStream(index = 2, type = MediaStreamType.AUDIO, language = "rus"),
	)

	for (language in listOf("rus", null)) {
		test("same item keeps the second audio track when remembered language is $language") {
			val itemId = UUID.randomUUID()
			val previous = VideoOptions().apply {
				this.itemId = itemId
				audioStreamIndex = 2
			}
			val next = VideoOptions().apply {
				this.itemId = itemId
				audioStreamIndex = previous.getAudioStreamIndex(itemId)
				applyAudioLanguage(language, streams)
			}

			next.audioStreamIndex shouldBe 2
		}
	}

	test("new item uses remembered language instead of previous item track index") {
		val previous = VideoOptions().apply {
			itemId = UUID.randomUUID()
			audioStreamIndex = 2
		}
		val next = VideoOptions().apply {
			itemId = UUID.randomUUID()
			audioStreamIndex = previous.getAudioStreamIndex(requireNotNull(itemId))
			applyAudioLanguage("rus", streams)
		}

		next.audioStreamIndex shouldBe 1
	}

	for (language in listOf("eng", null)) {
		test("no audio override when language is unavailable or disabled: $language") {
			val options = VideoOptions()
			options.applyAudioLanguage(language, streams)

			options.audioStreamIndex shouldBe null
		}
	}

	test("audio stream index is preserved when restarting the same item") {
		val itemId = UUID.randomUUID()
		val options = VideoOptions().apply {
			this.itemId = itemId
			audioStreamIndex = 2
		}

		options.getAudioStreamIndex(itemId) shouldBe 2
	}

	test("audio stream index is not carried over to the next item") {
		val options = VideoOptions().apply {
			itemId = UUID.randomUUID()
			audioStreamIndex = 1
		}

		options.getAudioStreamIndex(UUID.randomUUID()) shouldBe null
	}
})

private fun audioTestStream(index: Int, type: MediaStreamType, language: String) = MediaStream(
	index = index,
	type = type,
	language = language,
	isInterlaced = false,
	isDefault = false,
	isForced = false,
	isHearingImpaired = false,
	isExternal = false,
	isTextSubtitleStream = type == MediaStreamType.SUBTITLE,
	supportsExternalStream = false,
)
