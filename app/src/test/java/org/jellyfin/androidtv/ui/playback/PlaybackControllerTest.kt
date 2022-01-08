package org.jellyfin.androidtv.ui.playback

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.jellyfin.androidtv.preference.Preference
import org.jellyfin.androidtv.preference.PreferenceStore
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.KoinTest

class PlaybackControllerTest : KoinTest {
	// Mockk managed deps
	private val mockBaseItems = listOf(mockk<BaseItemDto>())
	private val mockFragment = mockk<IPlaybackOverlayFragment>()

	lateinit var playbackController: PlaybackController

	// Koin managed modules
	lateinit var mockApiClient: ApiClient
	lateinit var mockUserPreferences: PreferenceStore

	private fun prepDiMocks(): Module {
		mockApiClient = mockk(relaxed = true)
		mockUserPreferences = mockk(relaxed = true)

		// Required in the constructor
		every {
			mockUserPreferences.get(any<Preference<PreferredVideoPlayer>>())
		} returns PreferredVideoPlayer.EXOPLAYER

		return module {
			single { mockApiClient }
			single { mockUserPreferences }
		}
	}

	@Before
	fun setUp() {
		startKoin { modules(prepDiMocks()) }
		playbackController = PlaybackController(mockBaseItems, mockFragment)
	}

	@After
	fun tearDown() {
		unmockkAll()
		stopKoin()
	}

	@Test
	fun testPlaybackControllerConstruction() {
		assertNotNull(playbackController)
	}

	@Test
	fun testSetPlaybackSpeedForwardsToVideoManager() {
		doubleArrayOf(0.25, 0.5, 1.0, 2.5, 200.0).forEach { i ->
			val mockVideoManager = mockk<VideoManager>(relaxed = true)
			playbackController.mVideoManager = mockVideoManager
			every { mockVideoManager.isInitialized } returns true

			playbackController.setPlaybackSpeed(i)

			verify { mockVideoManager.setPlaybackSpeed(i) }
		}
	}

	@Test
	fun testSetupPlaybackSpeedHandlesNoVideo() {
		playbackController.mVideoManager = null
		playbackController.setPlaybackSpeed(2.0)
	}
}
