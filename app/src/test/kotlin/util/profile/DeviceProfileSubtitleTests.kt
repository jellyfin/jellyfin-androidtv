package org.jellyfin.androidtv.util.profile

import android.util.Size
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod

class DeviceProfileSubtitleTests : FunSpec({
	test("ASS and SSA profiles direct play when libass is enabled") {
		val profile = createDeviceProfile(
			mediaTest = testCapabilities(),
			maxBitrate = 100_000_000,
			isAC3Enabled = true,
			downMixAudio = false,
			assDirectPlay = true,
			pgsDirectPlay = false,
			userAVCLevel = null,
			userHEVCLevel = null,
		)

		profile.methodsFor(Codec.Subtitle.ASS) shouldContainExactlyInAnyOrder listOf(
			SubtitleDeliveryMethod.EMBED,
			SubtitleDeliveryMethod.EXTERNAL,
			SubtitleDeliveryMethod.ENCODE,
		)
		profile.methodsFor(Codec.Subtitle.SSA) shouldContainExactlyInAnyOrder listOf(
			SubtitleDeliveryMethod.EMBED,
			SubtitleDeliveryMethod.EXTERNAL,
			SubtitleDeliveryMethod.ENCODE,
		)
	}

	test("ASS and SSA profiles only encode when libass is disabled") {
		val profile = createDeviceProfile(
			mediaTest = testCapabilities(),
			maxBitrate = 100_000_000,
			isAC3Enabled = true,
			downMixAudio = false,
			assDirectPlay = false,
			pgsDirectPlay = false,
			userAVCLevel = null,
			userHEVCLevel = null,
		)

		profile.methodsFor(Codec.Subtitle.ASS) shouldContainExactlyInAnyOrder listOf(
			SubtitleDeliveryMethod.ENCODE
		)
		profile.methodsFor(Codec.Subtitle.SSA) shouldContainExactlyInAnyOrder listOf(
			SubtitleDeliveryMethod.ENCODE
		)
	}
})

private fun org.jellyfin.sdk.model.api.DeviceProfile.methodsFor(format: String) = subtitleProfiles
	.filter { it.format == format }
	.map { it.method }

private fun testCapabilities(): DeviceProfileCapabilities {
	val zeroSize = mockk<Size> {
		every { width } returns 0
		every { height } returns 0
	}

	return object : DeviceProfileCapabilities {
		override fun supportsAV1() = false
		override fun supportsAV1Main10() = false
		override fun supportsAV1DolbyVision() = false
		override fun supportsAV1HDR10() = false
		override fun supportsAV1HDR10Plus() = false
		override fun supportsAVC() = false
		override fun supportsAVCHigh10() = false
		override fun getAVCMainLevel() = 0
		override fun getAVCHigh10Level() = 0
		override fun supportsHevc() = false
		override fun supportsHevcMain10() = false
		override fun supportsHevcDolbyVision() = false
		override fun supportsHevcDolbyVisionEL() = false
		override fun supportsHevcHDR10() = false
		override fun supportsHevcHDR10Plus() = false
		override fun getHevcMainLevel() = 0
		override fun getHevcMain10Level() = 0
		override fun supportsVc1() = false
		override fun getMaxResolution(mime: String) = zeroSize
	}
}
