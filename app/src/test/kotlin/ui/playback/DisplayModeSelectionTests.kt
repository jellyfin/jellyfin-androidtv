package org.jellyfin.androidtv.ui.playback

import android.view.Display
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior.DISABLED
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior.SCALE_ON_DEVICE
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior.SCALE_ON_TV

class DisplayModeSelectionTests : FunSpec({
	fun mode(rate: Float, width: Int = 3840, height: Int = 2160) = mockk<Display.Mode>(relaxed = true) {
		every { refreshRate } returns rate
		every { physicalWidth } returns width
		every { physicalHeight } returns height
	}

	val filmRate = 24_000f / 1001
	val current = mode(30.000002f)
	val film = mode(filmRate)
	val cinema = mode(24.000002f)
	val ntsc = mode(60_000f / 1001)

	fun select(
		sourceRate: Float,
		vararg modes: Display.Mode,
		width: Int = 3840,
		height: Int = 2160,
		scaling: RefreshRateSwitchingBehavior = SCALE_ON_DEVICE,
	) = PlaybackController.findBestDisplayMode(modes, current, width, height, sourceRate, scaling)

	test("4K display advertising integer rates selects 24 Hz for both film rates") {
		val modes = arrayOf(
			current, mode(25f), cinema,
			mode(60f, 1920, 1080), mode(50f, 1920, 1080), mode(30f, 1920, 1080), mode(24.000002f, 1920, 1080),
			mode(60f, 1280, 720), mode(50f, 1280, 720),
		)
		select(24f, *modes) shouldBe cinema
		select(filmRate, *modes) shouldBe cinema
	}

	test("film prefers native refresh, then 24 Hz, then 59.94 Hz independent of enumeration order") {
		for (modes in listOf(arrayOf(film, cinema, ntsc), arrayOf(ntsc, cinema, film))) {
			select(filmRate, *modes) shouldBe film
			select(24f, *modes) shouldBe cinema
		}
		select(filmRate, cinema, ntsc) shouldBe cinema
		select(filmRate, ntsc, cinema) shouldBe cinema
		select(filmRate, ntsc) shouldBe ntsc
	}

	test("supports fractional rates and double-rate output") {
		select(30_000f / 1001, current) shouldBe current
		val sixty = mode(60f)
		select(60_000f / 1001, sixty) shouldBe sixty
		select(30_000f / 1001, sixty) shouldBe sixty
		val fifty = mode(50f)
		select(25f, fifty) shouldBe fifty
	}

	test("rejects incompatible rates and invalid source rates") {
		select(filmRate, current, mode(25f)) shouldBe null
		for (rate in listOf(0f, -24f, Float.NaN, Float.POSITIVE_INFINITY)) {
			select(rate, cinema) shouldBe null
		}
	}

	test("accepts rates within 0.11 percent of the target") {
		val inside = mode(24.026f)
		select(24f, inside) shouldBe inside
		select(24f, mode(24.027f)) shouldBe null
	}

	test("scaling preference resolves equal cadence at different resolutions") {
		val hd = mode(24f, 1920, 1080)
		select(filmRate, hd, cinema, width = 1920, height = 1080, scaling = SCALE_ON_DEVICE) shouldBe cinema
		select(filmRate, hd, cinema, width = 1920, height = 1080, scaling = SCALE_ON_TV) shouldBe hd
	}

	test("disabled switching leaves the display in its current mode") {
		select(24f, cinema, scaling = DISABLED) shouldBe null
	}

	test("rejects modes below source resolution or the HD minimum") {
		select(filmRate, mode(24f, 1920, 1080)) shouldBe null
		select(filmRate, mode(24f, 720, 480), width = 720, height = 480) shouldBe null
	}

	test("TV scaling weighs resolution difference alongside refresh rate") {
		val hdDouble = mode(48f, 1280, 720)
		select(24f, cinema, hdDouble, width = 1280, height = 720, scaling = SCALE_ON_TV) shouldBe hdDouble
	}
})
