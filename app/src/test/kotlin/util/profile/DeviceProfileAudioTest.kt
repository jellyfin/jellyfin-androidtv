package org.jellyfin.androidtv.util.profile

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.sdk.model.api.DlnaProfileType

class DeviceProfileAudioTest : FunSpec({
	fun profileFor(
		passthrough: Set<String>,
		lossless: Boolean = false,
		isAC3Enabled: Boolean = true,
		downMixAudio: Boolean = false,
	) = createDeviceProfile(
		mediaTest = mockk(relaxed = true),
		passthroughSupport = PassthroughSupport(passthrough, lossless),
		maxBitrate = 100_000_000,
		isAC3Enabled = isAC3Enabled,
		downMixAudio = downMixAudio,
		assDirectPlay = true,
		pgsDirectPlay = true,
		userAVCLevel = null,
		userHEVCLevel = null,
		forceEnabledHdr = emptySet(),
		forceDisabledHdr = emptySet(),
	)

	fun declaredCodecs(profile: org.jellyfin.sdk.model.api.DeviceProfile) = profile.directPlayProfiles
		.mapNotNull { it.audioCodec }
		.flatMap { it.split(',') }
		.toSet()

	val compressedOnly = setOf(Codec.Audio.AC3, Codec.Audio.EAC3)

	test("drops surround codecs the device cannot pass through") {
		val codecs = declaredCodecs(profileFor(compressedOnly))
		codecs.contains(Codec.Audio.TRUEHD) shouldBe false
		codecs.contains(Codec.Audio.DTS) shouldBe false
		codecs.contains(Codec.Audio.AC3) shouldBe true
		codecs.contains(Codec.Audio.AAC) shouldBe true
	}

	test("keeps everything when the device passes lossless through") {
		val codecs = declaredCodecs(profileFor(compressedOnly + Codec.Audio.TRUEHD, lossless = true))
		codecs.contains(Codec.Audio.TRUEHD) shouldBe true
		codecs.contains(Codec.Audio.DTS) shouldBe true
	}

	test("keeps everything when the device passes nothing through") {
		declaredCodecs(profileFor(emptySet())).contains(Codec.Audio.TRUEHD) shouldBe true
	}

	test("keeps everything when the user switched the Dolby codecs off") {
		val codecs = declaredCodecs(profileFor(compressedOnly, isAC3Enabled = false))
		codecs.contains(Codec.Audio.AC3) shouldBe false
		codecs.contains(Codec.Audio.TRUEHD) shouldBe true
	}

	test("asks the server for AC3 first, which ffmpeg encodes better than EAC3") {
		profileFor(compressedOnly).transcodingProfiles
			.filter { it.type == DlnaProfileType.VIDEO }
			.forEach { it.audioCodec?.split(',')?.first() shouldBe Codec.Audio.AC3 }
	}

	test("asks for AAC when downmixing to stereo") {
		profileFor(compressedOnly, downMixAudio = true).transcodingProfiles
			.filter { it.type == DlnaProfileType.VIDEO }
			.forEach { it.audioCodec?.split(',')?.first() shouldBe Codec.Audio.AAC }
	}

	test("keeps every codec list inside the 40 characters the server validates against") {
		profileFor(compressedOnly).transcodingProfiles
			.mapNotNull { it.audioCodec }
			.forEach { it.length shouldBeLessThanOrEqual 40 }
	}
})
