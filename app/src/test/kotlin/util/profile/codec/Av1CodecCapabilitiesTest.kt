package org.jellyfin.androidtv.util.profile.codec

import android.media.MediaCodecInfo.CodecProfileLevel
import android.os.Build
import androidx.media3.common.MimeTypes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class Av1CodecCapabilitiesTest : FunSpec({
	val mimeAv1 = MimeTypes.VIDEO_AV1
	val mimeDolbyVision = MimeTypes.VIDEO_DOLBY_VISION
	val apiN = Build.VERSION_CODES.N
	val apiQ = Build.VERSION_CODES.Q
	val apiR = Build.VERSION_CODES.R

	test("supportsAv1 returns true when device has AV1 decoder") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeAv1) } returns true
		}
		Av1CodecCapabilities(query).supportsAv1() shouldBe true
	}

	test("supportsAv1 returns false when device has no AV1 decoder") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeAv1) } returns false
		}
		Av1CodecCapabilities(query).supportsAv1() shouldBe false
	}

	test("supportsAv1Main10 returns true when device supports Main10 at Level 5") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeAv1, any(), any()) } returns true
		}
		Av1CodecCapabilities(query, sdkInt = apiQ).supportsAv1Main10() shouldBe true
	}

	test("supportsAv1Main10 returns false when device lacks Main10 support") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeAv1, any(), any()) } returns false
		}
		Av1CodecCapabilities(query, sdkInt = apiQ).supportsAv1Main10() shouldBe false
	}

	test("supportsAv1Main10 uses fallback constants on pre-Q devices") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(any(), any(), any()) } returns true
		}
		Av1CodecCapabilities(query, sdkInt = apiQ - 1).supportsAv1Main10()

		verify {
			query.hasDecoder(
				mimeAv1,
				Av1CodecCapabilities.AV1_PROFILE_MAIN10,
				Av1CodecCapabilities.AV1_LEVEL5,
			)
		}
	}

	test("supportsAv1DolbyVision returns false when sdkInt below API 24") {
		val query = mockk<MediaCodecQuery>()
		Av1CodecCapabilities(query, sdkInt = apiN - 1).supportsAv1DolbyVision() shouldBe false
	}

	test("supportsAv1DolbyVision returns true when sdkInt >= 24 and device has DV decoder") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeDolbyVision, any(), any()) } returns true
		}
		Av1CodecCapabilities(query, sdkInt = apiN).supportsAv1DolbyVision() shouldBe true
	}

	test("supportsAv1DolbyVision returns false when sdkInt >= 24 but no DV decoder") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeDolbyVision, any(), any()) } returns false
		}
		Av1CodecCapabilities(query, sdkInt = apiN).supportsAv1DolbyVision() shouldBe false
	}

	test("supportsAv1DolbyVision uses fallback DV constant on pre-R devices") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(any(), any(), any()) } returns true
		}
		Av1CodecCapabilities(query, sdkInt = apiN).supportsAv1DolbyVision()

		verify {
			query.hasDecoder(
				mimeDolbyVision,
				Av1CodecCapabilities.DV_PROFILE_DVAV1_10,
				CodecProfileLevel.DolbyVisionLevelHd24,
			)
		}
	}

	test("supportsAv1HDR10 returns true when device supports Main10 HDR10") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeAv1, any(), any()) } returns true
		}
		Av1CodecCapabilities(query, sdkInt = apiQ).supportsAv1HDR10() shouldBe true
	}

	test("supportsAv1HDR10 returns false when device lacks Main10 HDR10") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeAv1, any(), any()) } returns false
		}
		Av1CodecCapabilities(query, sdkInt = apiQ).supportsAv1HDR10() shouldBe false
	}

	test("supportsAv1HDR10 uses fallback constants on pre-Q devices") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(any(), any(), any()) } returns true
		}
		Av1CodecCapabilities(query, sdkInt = apiQ - 1).supportsAv1HDR10()

		verify {
			query.hasDecoder(
				mimeAv1,
				Av1CodecCapabilities.AV1_PROFILE_MAIN10_HDR10,
				Av1CodecCapabilities.AV1_LEVEL5,
			)
		}
	}

	test("supportsAv1HDR10Plus returns true when device supports Main10 HDR10Plus") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeAv1, any(), any()) } returns true
		}
		Av1CodecCapabilities(query, sdkInt = apiQ).supportsAv1HDR10Plus() shouldBe true
	}

	test("supportsAv1HDR10Plus returns false when device lacks Main10 HDR10Plus") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(mimeAv1, any(), any()) } returns false
		}
		Av1CodecCapabilities(query, sdkInt = apiQ).supportsAv1HDR10Plus() shouldBe false
	}

	test("supportsAv1HDR10Plus uses fallback constants on pre-Q devices") {
		val query = mockk<MediaCodecQuery> {
			every { hasDecoder(any(), any(), any()) } returns true
		}
		Av1CodecCapabilities(query, sdkInt = apiQ - 1).supportsAv1HDR10Plus()

		verify {
			query.hasDecoder(
				mimeAv1,
				Av1CodecCapabilities.AV1_PROFILE_MAIN10_HDR10_PLUS,
				Av1CodecCapabilities.AV1_LEVEL5,
			)
		}
	}

	// Simulated device profiles
	test("Modern device profile: full AV1 + DolbyVision + HDR10+") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeAv1) } returns true
			every { hasDecoder(mimeAv1, any(), any()) } returns true
			every { hasDecoder(mimeDolbyVision, any(), any()) } returns true
		}
		val av1 = Av1CodecCapabilities(query, sdkInt = apiR)

		av1.supportsAv1() shouldBe true
		av1.supportsAv1Main10() shouldBe true
		av1.supportsAv1DolbyVision() shouldBe true
		av1.supportsAv1HDR10() shouldBe true
		av1.supportsAv1HDR10Plus() shouldBe true
	}

	test("Fire TV profile: AV1 on older API with fallback constants, no DolbyVision") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeAv1) } returns true
			every { hasDecoder(mimeAv1, Av1CodecCapabilities.AV1_PROFILE_MAIN10, Av1CodecCapabilities.AV1_LEVEL5) } returns true
			every { hasDecoder(mimeAv1, Av1CodecCapabilities.AV1_PROFILE_MAIN10_HDR10, Av1CodecCapabilities.AV1_LEVEL5) } returns true
			every { hasDecoder(mimeAv1, Av1CodecCapabilities.AV1_PROFILE_MAIN10_HDR10_PLUS, Av1CodecCapabilities.AV1_LEVEL5) } returns false
			every { hasDecoder(mimeDolbyVision, any(), any()) } returns false
		}
		val av1 = Av1CodecCapabilities(query, sdkInt = apiN)

		av1.supportsAv1() shouldBe true
		av1.supportsAv1Main10() shouldBe true
		av1.supportsAv1DolbyVision() shouldBe false
		av1.supportsAv1HDR10() shouldBe true
		av1.supportsAv1HDR10Plus() shouldBe false
	}

	test("No AV1 support at all") {
		val query = mockk<MediaCodecQuery> {
			every { hasCodecForMime(mimeAv1) } returns false
			every { hasDecoder(mimeAv1, any(), any()) } returns false
			every { hasDecoder(mimeDolbyVision, any(), any()) } returns false
		}
		val av1 = Av1CodecCapabilities(query, sdkInt = apiR)

		av1.supportsAv1() shouldBe false
		av1.supportsAv1Main10() shouldBe false
		av1.supportsAv1DolbyVision() shouldBe false
		av1.supportsAv1HDR10() shouldBe false
		av1.supportsAv1HDR10Plus() shouldBe false
	}
})
