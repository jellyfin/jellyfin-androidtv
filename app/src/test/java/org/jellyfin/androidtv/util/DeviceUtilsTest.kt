package org.jellyfin.androidtv.util

import android.os.Build
import io.mockk.*
import org.junit.After
import org.junit.Test
import org.junit.Assert.*

class DeviceUtilsTest {
	@After
	fun tearDown(){
		unmockkAll()
	}

	@Test
	fun testBuildModelIsNull(){
		// A number of the tests below rely on the assumption
		// that Build.MODEL is null in unit tests
		assertNull(Build.MODEL)
	}

	@Test
	fun testMethodsCanHandleNullModel(){
		// Methods that implicitly rely on Build.MODEL
		val methods = arrayOf(
			DeviceUtils::isChromecastWithGoogleTV,
			DeviceUtils::isFireTv,
			DeviceUtils::isFireTvStickGen1,
			DeviceUtils::isFireTvStick4k,
			DeviceUtils::isShieldTv,
			DeviceUtils::has4kVideoSupport
		)

		for(method in methods){
			assertFalse(method())
		}
	}

	private fun mockBuildModel(mockedVal: String?){
		mockkStatic(DeviceUtils::class)
		every { DeviceUtils.getBuildModel() } returns mockedVal
	}

	@Test
	fun testIsChromecastGoogleTvTrue(){
		mockBuildModel("Chromecast")
		assertTrue(DeviceUtils.isChromecastWithGoogleTV())
	}

	@Test
	fun testIsFireTV(){
		val acceptableInputs = arrayOf("AFT", "AFT_foo", "AFT ", "AFT2")
		for (input in acceptableInputs){
			mockBuildModel(input)
			assertTrue(DeviceUtils.isFireTv())
		}
	}

	@Test
	fun testIsFireTVGen1(){
		mockBuildModel("AFTM")
		assertTrue(DeviceUtils.isFireTv())
		assertTrue(DeviceUtils.isFireTvStickGen1())
		assertFalse(DeviceUtils.isFireTvStick4k())
	}

	@Test
	fun testIsFireTV4k(){
		mockBuildModel("AFTMM")
		assertTrue(DeviceUtils.isFireTv())
		assertTrue(DeviceUtils.isFireTvStick4k())
		assertFalse(DeviceUtils.isFireTvStickGen1())
	}

	@Test
	fun testIsShieldTv(){
		mockBuildModel("SHIELD Android TV")
		assertTrue(DeviceUtils.isShieldTv())
	}

	@Test
	fun testDisabled4kModels(){
		val fire1080pSticks = arrayOf(
			"AFTM", "AFTT", "AFTSSS", "AFTSS", "AFTB", "AFTS"
		)
		for (input in fire1080pSticks){
			mockBuildModel(input)
			assertFalse(input, DeviceUtils.has4kVideoSupport())
		}
	}
}
