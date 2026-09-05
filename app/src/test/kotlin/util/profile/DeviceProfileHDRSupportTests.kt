package org.jellyfin.androidtv.util.profile

import android.util.Size
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.jellyfin.androidtv.preference.constant.HDRSupport
import org.jellyfin.sdk.model.api.CodecProfile
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.api.VideoRangeType

class DeviceProfileHDRSupportTests : FunSpec({
	beforeEach {
		mockkObject(KnownDefects)
		every { KnownDefects.hevcDoviHdr10PlusBug } returns false
		every { KnownDefects.unreportedDoviProfile7Support } returns false
	}

	afterEach {
		unmockkObject(KnownDefects)
	}

	test("automatic preserves platform-derived AV1 and HEVC HDR exclusions") {
		val profile = createProfile(HDRSupport.AUTOMATIC)

		profile.hdrExclusionsByCodec() shouldContainExactly mapOf(
			"av1" to setOf(
				VideoRangeType.DOVI_INVALID,
				VideoRangeType.DOVI,
				VideoRangeType.DOVI_WITH_HDR10,
				VideoRangeType.DOVI_WITH_HDR10_PLUS,
				VideoRangeType.HDR10_PLUS,
				VideoRangeType.HDR10,
			),
			"hevc" to setOf(
				VideoRangeType.DOVI_INVALID,
				VideoRangeType.DOVI_WITH_EL,
				VideoRangeType.DOVI_WITH_ELHDR10_PLUS,
				VideoRangeType.DOVI,
				VideoRangeType.DOVI_WITH_HDR10,
				VideoRangeType.DOVI_WITH_HDR10_PLUS,
				VideoRangeType.HDR10_PLUS,
				VideoRangeType.HDR10,
			),
		)
	}

	test("enabled permits valid HDR ranges but still rejects malformed Dolby Vision") {
		val profile = createProfile(HDRSupport.ENABLED)

		profile.hdrExclusionsByCodec() shouldContainExactly mapOf(
			"av1" to setOf(VideoRangeType.DOVI_INVALID),
			"hevc" to setOf(VideoRangeType.DOVI_INVALID),
		)
	}

	test("disabled rejects every non-SDR range for every advertised video codec") {
		val profile = createProfile(HDRSupport.DISABLED)
		val advertisedVideoCodecs = profile.directPlayProfiles
			.single { it.type == DlnaProfileType.VIDEO }
			.videoCodec.orEmpty()
			.split(',')
			.toSet()
		val expectedRanges = VideoRangeType.entries.filterNot { it == VideoRangeType.SDR }.toSet()
		val exclusions = profile.hdrExclusionsByCodec()

		exclusions.keys shouldBe advertisedVideoCodecs
		profile.hdrApplyConditionsByCodec() shouldBe exclusions
		for (codec in advertisedVideoCodecs) {
			exclusions.getValue(codec) shouldBe expectedRanges
			exclusions.getValue(codec) shouldNotContain VideoRangeType.SDR
		}
	}

	test("HDR overrides leave non-HDR codec restrictions unchanged") {
		val automatic = createProfile(HDRSupport.AUTOMATIC)
		val enabled = createProfile(HDRSupport.ENABLED)
		val disabled = createProfile(HDRSupport.DISABLED)

		automatic.nonHDRCodecProfiles() shouldBe enabled.nonHDRCodecProfiles()
		automatic.nonHDRCodecProfiles() shouldBe disabled.nonHDRCodecProfiles()
		automatic.codecProfiles.flatMap(CodecProfile::conditions).map { it.property } shouldContain ProfileConditionValue.WIDTH
	}
})

private fun createProfile(hdrSupport: HDRSupport): DeviceProfile {
	val resolution = mockk<Size> {
		every { width } returns 1920
		every { height } returns 1080
	}
	val mediaTest = mockk<MediaCodecCapabilitiesTest>(relaxed = true) {
		every { getMaxResolution(any()) } returns resolution
	}

	return createDeviceProfile(
		mediaTest = mediaTest,
		maxBitrate = 100_000_000,
		isAC3Enabled = true,
		downMixAudio = false,
		assDirectPlay = false,
		pgsDirectPlay = true,
		userAVCLevel = 51,
		userHEVCLevel = 51,
		hdrSupport = hdrSupport,
	)
}

private fun DeviceProfile.hdrExclusionsByCodec(): Map<String, Set<VideoRangeType>> = codecProfiles
	.filter { profile -> profile.conditions.any { it.property == ProfileConditionValue.VIDEO_RANGE_TYPE } }
	.associate { profile ->
		profile.codec.orEmpty() to profile.conditions
			.single { it.property == ProfileConditionValue.VIDEO_RANGE_TYPE }
			.value
			.orEmpty()
			.split('|')
			.map(VideoRangeType::fromName)
			.toSet()
	}

private fun DeviceProfile.nonHDRCodecProfiles(): List<CodecProfile> = codecProfiles.filterNot { profile ->
	profile.conditions.any { it.property == ProfileConditionValue.VIDEO_RANGE_TYPE }
}

private fun DeviceProfile.hdrApplyConditionsByCodec(): Map<String, Set<VideoRangeType>> = codecProfiles
	.filter { profile -> profile.applyConditions.any { it.property == ProfileConditionValue.VIDEO_RANGE_TYPE } }
	.associate { profile ->
		profile.codec.orEmpty() to profile.applyConditions
			.single { it.property == ProfileConditionValue.VIDEO_RANGE_TYPE }
			.value
			.orEmpty()
			.split('|')
			.map(VideoRangeType::fromName)
			.toSet()
	}
