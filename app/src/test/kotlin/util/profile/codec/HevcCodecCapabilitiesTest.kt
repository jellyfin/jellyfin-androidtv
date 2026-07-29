package org.jellyfin.androidtv.util.profile.codec

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaFormat
import android.os.Build
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.jellyfin.androidtv.util.AndroidVersion

class HevcCodecCapabilitiesTest : FunSpec({
	val mimeHevc = MediaFormat.MIMETYPE_VIDEO_HEVC
	val mimeDolbyVision = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION
	val apiN = Build.VERSION_CODES.N
	val apiQ = Build.VERSION_CODES.Q

	beforeEach {
		mockkObject(AndroidVersion)
	}

	afterEach {
		unmockkObject(AndroidVersion)
	}

	test("supportsHevc returns true when device has HEVC decoder") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeHevc) } returns true
		}
		HevcCodecCapabilities(query).supportsHevc() shouldBe true
	}

	test("supportsHevc returns false when device has no HEVC decoder") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeHevc) } returns false
		}
		HevcCodecCapabilities(query).supportsHevc() shouldBe false
	}

	test("supportsHevcMain10 returns true when device supports Main10 at Level 4") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10, CodecProfileLevel.HEVCMainTierLevel4) } returns true
		}
		HevcCodecCapabilities(query).supportsHevcMain10() shouldBe true
	}

	test("supportsHevcMain10 returns false when device lacks Main10 support") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10, CodecProfileLevel.HEVCMainTierLevel4) } returns false
		}
		HevcCodecCapabilities(query).supportsHevcMain10() shouldBe false
	}

	test("supportsHevcDolbyVision returns false when sdkInt below API 24") {
		every { AndroidVersion.sdkInt } returns apiN - 1
		val query = mockk<MediaCodecQuery>()
		HevcCodecCapabilities(query).supportsHevcDolbyVision() shouldBe false
	}

	test("supportsHevcDolbyVision returns true when sdkInt >= 24 and device has Dolby Vision codec") {
		every { AndroidVersion.sdkInt } returns apiN
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeDolbyVision) } returns true
		}
		HevcCodecCapabilities(query).supportsHevcDolbyVision() shouldBe true
	}

	test("supportsHevcDolbyVision returns false when sdkInt >= 24 but no Dolby Vision codec") {
		every { AndroidVersion.sdkInt } returns apiN
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeDolbyVision) } returns false
		}
		HevcCodecCapabilities(query).supportsHevcDolbyVision() shouldBe false
	}

	test("supportsHevcDolbyVisionEL returns false when sdkInt below API 24") {
		every { AndroidVersion.sdkInt } returns apiN - 1
		val query = mockk<MediaCodecQuery>()
		HevcCodecCapabilities(query).supportsHevcDolbyVisionEL() shouldBe false
	}

	test("supportsHevcDolbyVisionEL returns true when device has Profile 7 decoder and multi-instance HEVC") {
		every { AndroidVersion.sdkInt } returns apiN
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeDolbyVision, CodecProfileLevel.DolbyVisionProfileDvheDtb, CodecProfileLevel.DolbyVisionLevelHd24) } returns true
			every { supportsMultiInstance(mimeHevc) } returns true
		}
		HevcCodecCapabilities(query).supportsHevcDolbyVisionEL() shouldBe true
	}

	test("supportsHevcDolbyVisionEL returns false when device has Profile 7 but no multi-instance") {
		every { AndroidVersion.sdkInt } returns apiN
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeDolbyVision, CodecProfileLevel.DolbyVisionProfileDvheDtb, CodecProfileLevel.DolbyVisionLevelHd24) } returns true
			every { supportsMultiInstance(mimeHevc) } returns false
		}
		HevcCodecCapabilities(query).supportsHevcDolbyVisionEL() shouldBe false
	}

	test("supportsHevcDolbyVisionEL returns false when device lacks Profile 7 decoder") {
		every { AndroidVersion.sdkInt } returns apiN
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeDolbyVision, CodecProfileLevel.DolbyVisionProfileDvheDtb, CodecProfileLevel.DolbyVisionLevelHd24) } returns false
		}
		HevcCodecCapabilities(query).supportsHevcDolbyVisionEL() shouldBe false
	}

	test("supportsHevcHDR10 returns false when sdkInt below API 24") {
		every { AndroidVersion.sdkInt } returns apiN - 1
		val query = mockk<MediaCodecQuery>()
		HevcCodecCapabilities(query).supportsHevcHDR10() shouldBe false
	}

	test("supportsHevcHDR10 returns true when sdkInt >= 24 and device supports Main10 HDR10") {
		every { AndroidVersion.sdkInt } returns apiN
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10HDR10, CodecProfileLevel.HEVCMainTierLevel4) } returns true
		}
		HevcCodecCapabilities(query).supportsHevcHDR10() shouldBe true
	}

	test("supportsHevcHDR10 returns false when sdkInt >= 24 but device lacks Main10 HDR10") {
		every { AndroidVersion.sdkInt } returns apiN
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10HDR10, CodecProfileLevel.HEVCMainTierLevel4) } returns false
		}
		HevcCodecCapabilities(query).supportsHevcHDR10() shouldBe false
	}

	test("supportsHevcHDR10Plus returns false when sdkInt below API 29") {
		every { AndroidVersion.sdkInt } returns apiQ - 1
		val query = mockk<MediaCodecQuery>()
		HevcCodecCapabilities(query).supportsHevcHDR10Plus() shouldBe false
	}

	test("supportsHevcHDR10Plus returns true when sdkInt >= 29 and device supports Main10 HDR10Plus") {
		every { AndroidVersion.sdkInt } returns apiQ
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10HDR10Plus, CodecProfileLevel.HEVCMainTierLevel4) } returns true
		}
		HevcCodecCapabilities(query).supportsHevcHDR10Plus() shouldBe true
	}

	test("supportsHevcHDR10Plus returns false when sdkInt >= 29 but device lacks Main10 HDR10Plus") {
		every { AndroidVersion.sdkInt } returns apiQ
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10HDR10Plus, CodecProfileLevel.HEVCMainTierLevel4) } returns false
		}
		HevcCodecCapabilities(query).supportsHevcHDR10Plus() shouldBe false
	}

	test("getMainLevel returns mapped level for device with Main Level 4.1") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain) } returns CodecProfileLevel.HEVCMainTierLevel41
		}
		HevcCodecCapabilities(query).getMainLevel() shouldBe 123
	}

	test("getMainLevel returns mapped level for device with Main Level 5.1") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain) } returns CodecProfileLevel.HEVCMainTierLevel51
		}
		HevcCodecCapabilities(query).getMainLevel() shouldBe 153
	}

	test("getMainLevel returns mapped level for device with Main Level 6.2") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain) } returns CodecProfileLevel.HEVCMainTierLevel62
		}
		HevcCodecCapabilities(query).getMainLevel() shouldBe 186
	}

	test("getMainLevel returns 0 when device reports no HEVC Main support") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain) } returns 0
		}
		HevcCodecCapabilities(query).getMainLevel() shouldBe 0
	}

	test("getMainLevel maps level between known values to the lower known level") {
		val query = mockk<MediaCodecQuery> {
			// Return a value between HEVCMainTierLevel4 and HEVCMainTierLevel41
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain) } returns (CodecProfileLevel.HEVCMainTierLevel4 + 1)
		}
		HevcCodecCapabilities(query).getMainLevel() shouldBe 120
	}

	test("getMain10Level returns mapped level for device with Main10 Level 5.1") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain10) } returns CodecProfileLevel.HEVCMainTierLevel51
		}
		HevcCodecCapabilities(query).getMain10Level() shouldBe 153
	}

	test("getMain10Level returns 0 when device has no Main10 support") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain10) } returns 0
		}
		HevcCodecCapabilities(query).getMain10Level() shouldBe 0
	}

	test("LEVEL_MAP contains all expected HEVC levels in ascending order") {
		val expectedFfprobeLevels = listOf(30, 60, 63, 90, 93, 120, 123, 150, 153, 156, 180, 183, 186)
		HevcCodecCapabilities.LEVEL_MAP.map { it.second } shouldBe expectedFfprobeLevels
	}

	test("LEVEL_MAP ffprobe level values are in ascending order") {
		val ffprobeLevels = HevcCodecCapabilities.LEVEL_MAP.map { it.second }
		ffprobeLevels shouldBe ffprobeLevels.sorted()
	}

	// Simulated device profiles
	test("Shield TV profile: Main10 + HDR10 + Level 5.1") {
		every { AndroidVersion.sdkInt } returns apiQ
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeHevc) } returns true
			every { hasCodecForMime(mimeDolbyVision) } returns true
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10, CodecProfileLevel.HEVCMainTierLevel4) } returns true
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10HDR10, CodecProfileLevel.HEVCMainTierLevel4) } returns true
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10HDR10Plus, CodecProfileLevel.HEVCMainTierLevel4) } returns false
			every { hasDecoder(mimeDolbyVision, CodecProfileLevel.DolbyVisionProfileDvheDtb, CodecProfileLevel.DolbyVisionLevelHd24) } returns false
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain) } returns CodecProfileLevel.HEVCMainTierLevel51
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain10) } returns CodecProfileLevel.HEVCMainTierLevel51
		}
		val hevc = HevcCodecCapabilities(query)

		hevc.supportsHevc() shouldBe true
		hevc.supportsHevcMain10() shouldBe true
		hevc.supportsHevcDolbyVision() shouldBe true
		hevc.supportsHevcDolbyVisionEL() shouldBe false
		hevc.supportsHevcHDR10() shouldBe true
		hevc.supportsHevcHDR10Plus() shouldBe false
		hevc.getMainLevel() shouldBe 153
		hevc.getMain10Level() shouldBe 153
	}

	test("Budget device profile: basic HEVC only, no HDR") {
		every { AndroidVersion.sdkInt } returns apiN
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeHevc) } returns true
			every { hasCodecForMime(mimeDolbyVision) } returns false
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10, CodecProfileLevel.HEVCMainTierLevel4) } returns false
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10HDR10, CodecProfileLevel.HEVCMainTierLevel4) } returns false
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10HDR10Plus, CodecProfileLevel.HEVCMainTierLevel4) } returns false
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain) } returns CodecProfileLevel.HEVCMainTierLevel41
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain10) } returns 0
		}
		val hevc = HevcCodecCapabilities(query)

		hevc.supportsHevc() shouldBe true
		hevc.supportsHevcMain10() shouldBe false
		hevc.supportsHevcDolbyVision() shouldBe false
		hevc.supportsHevcHDR10() shouldBe false
		hevc.supportsHevcHDR10Plus() shouldBe false
		hevc.getMainLevel() shouldBe 123
		hevc.getMain10Level() shouldBe 0
	}

	test("No HEVC support at all") {
		every { AndroidVersion.sdkInt } returns apiQ
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeHevc) } returns false
			every { hasCodecForMime(mimeDolbyVision) } returns false
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10, CodecProfileLevel.HEVCMainTierLevel4) } returns false
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10HDR10, CodecProfileLevel.HEVCMainTierLevel4) } returns false
			every { hasDecoder(mimeHevc, CodecProfileLevel.HEVCProfileMain10HDR10Plus, CodecProfileLevel.HEVCMainTierLevel4) } returns false
			every { hasDecoder(mimeDolbyVision, CodecProfileLevel.DolbyVisionProfileDvheDtb, CodecProfileLevel.DolbyVisionLevelHd24) } returns false
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain) } returns 0
			every { getDecoderLevel(mimeHevc, CodecProfileLevel.HEVCProfileMain10) } returns 0
		}
		val hevc = HevcCodecCapabilities(query)

		hevc.supportsHevc() shouldBe false
		hevc.supportsHevcMain10() shouldBe false
		hevc.supportsHevcDolbyVision() shouldBe false
		hevc.supportsHevcDolbyVisionEL() shouldBe false
		hevc.supportsHevcHDR10() shouldBe false
		hevc.supportsHevcHDR10Plus() shouldBe false
		hevc.getMainLevel() shouldBe 0
		hevc.getMain10Level() shouldBe 0
	}
})
