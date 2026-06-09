package org.jellyfin.androidtv.util.profile.codec

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaFormat
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class AvcCodecCapabilitiesTest : FunSpec({
	val mime = MediaFormat.MIMETYPE_VIDEO_AVC

	test("supportsAvc returns true when device has AVC decoder") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mime) } returns true
		}
		AvcCodecCapabilities(query).supportsAvc() shouldBe true
	}

	test("supportsAvc returns false when device has no AVC decoder") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mime) } returns false
		}
		AvcCodecCapabilities(query).supportsAvc() shouldBe false
	}

	test("supportsAvcHigh10 returns true when device supports High10 at Level 4") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mime, CodecProfileLevel.AVCProfileHigh10, CodecProfileLevel.AVCLevel4) } returns true
		}
		AvcCodecCapabilities(query).supportsAvcHigh10() shouldBe true
	}

	test("supportsAvcHigh10 returns false when device lacks High10 support") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mime, CodecProfileLevel.AVCProfileHigh10, CodecProfileLevel.AVCLevel4) } returns false
		}
		AvcCodecCapabilities(query).supportsAvcHigh10() shouldBe false
	}

	test("getMainLevel returns mapped level for device with Main Level 4.1") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileMain) } returns CodecProfileLevel.AVCLevel41
		}
		AvcCodecCapabilities(query).getMainLevel() shouldBe 41
	}

	test("getMainLevel returns mapped level for device with Main Level 5.1") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileMain) } returns CodecProfileLevel.AVCLevel51
		}
		AvcCodecCapabilities(query).getMainLevel() shouldBe 51
	}

	test("getMainLevel returns mapped level for device with Main Level 5.2") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileMain) } returns CodecProfileLevel.AVCLevel52
		}
		AvcCodecCapabilities(query).getMainLevel() shouldBe 52
	}

	test("getMainLevel returns 0 when device reports no AVC Main support") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileMain) } returns 0
		}
		AvcCodecCapabilities(query).getMainLevel() shouldBe 0
	}

	test("getMainLevel maps level between known values to the lower known level") {
		val query = mockk<MediaCodecQuery> {
			// Return a value between AVCLevel4 (2048) and AVCLevel41 (4096)
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileMain) } returns 3000
		}
		// 3000 >= AVCLevel4 (2048) but < AVCLevel41 (4096), so should map to 40
		AvcCodecCapabilities(query).getMainLevel() shouldBe 40
	}

	test("getMainLevel maps Level 1b correctly") {
		val query = mockk<MediaCodecQuery> {
			// AVCLevel1b = 2, AVCLevel1 = 1
			// In reversed search, AVCLevel1 (1) appears before AVCLevel1b (2)
			// so level 2 >= 1 matches AVCLevel1 first, returning 10
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileMain) } returns CodecProfileLevel.AVCLevel1b
		}
		AvcCodecCapabilities(query).getMainLevel() shouldBe 10
	}

	test("getMainLevel maps Level 1 correctly") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileMain) } returns CodecProfileLevel.AVCLevel1
		}
		AvcCodecCapabilities(query).getMainLevel() shouldBe 10
	}

	test("getHigh10Level returns mapped level for device with High10 Level 5.1") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileHigh10) } returns CodecProfileLevel.AVCLevel51
		}
		AvcCodecCapabilities(query).getHigh10Level() shouldBe 51
	}

	test("getHigh10Level returns 0 when device has no High10 support") {
		val query = mockk<MediaCodecQuery> {
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileHigh10) } returns 0
		}
		AvcCodecCapabilities(query).getHigh10Level() shouldBe 0
	}

	test("LEVEL_MAP contains all expected AVC levels in ascending order") {
		val expectedFfprobeLevels = listOf(9, 10, 11, 12, 13, 20, 21, 22, 30, 31, 32, 40, 41, 42, 50, 51, 52)
		AvcCodecCapabilities.LEVEL_MAP.map { it.second } shouldBe expectedFfprobeLevels
	}

	test("LEVEL_MAP ffprobe level values are in ascending order") {
		val ffprobeLevels = AvcCodecCapabilities.LEVEL_MAP.map { it.second }
		ffprobeLevels shouldBe ffprobeLevels.sorted()
	}

	// Simulated device profiles
	test("Shield TV profile: Main Level 5.1 + High10 Level 5.1") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mime) } returns true
			every { hasDecoder(mime, CodecProfileLevel.AVCProfileHigh10, CodecProfileLevel.AVCLevel4) } returns true
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileMain) } returns CodecProfileLevel.AVCLevel51
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileHigh10) } returns CodecProfileLevel.AVCLevel51
		}
		val avc = AvcCodecCapabilities(query)

		avc.supportsAvc() shouldBe true
		avc.supportsAvcHigh10() shouldBe true
		avc.getMainLevel() shouldBe 51
		avc.getHigh10Level() shouldBe 51
	}

	test("Budget Fire Stick profile: Main Level 4.1 only") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mime) } returns true
			every { hasDecoder(mime, CodecProfileLevel.AVCProfileHigh10, CodecProfileLevel.AVCLevel4) } returns false
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileMain) } returns CodecProfileLevel.AVCLevel41
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileHigh10) } returns 0
		}
		val avc = AvcCodecCapabilities(query)

		avc.supportsAvc() shouldBe true
		avc.supportsAvcHigh10() shouldBe false
		avc.getMainLevel() shouldBe 41
		avc.getHigh10Level() shouldBe 0
	}

	test("No AVC support at all") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mime) } returns false
			every { hasDecoder(mime, CodecProfileLevel.AVCProfileHigh10, CodecProfileLevel.AVCLevel4) } returns false
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileMain) } returns 0
			every { getDecoderLevel(mime, CodecProfileLevel.AVCProfileHigh10) } returns 0
		}
		val avc = AvcCodecCapabilities(query)

		avc.supportsAvc() shouldBe false
		avc.supportsAvcHigh10() shouldBe false
		avc.getMainLevel() shouldBe 0
		avc.getHigh10Level() shouldBe 0
	}
})
