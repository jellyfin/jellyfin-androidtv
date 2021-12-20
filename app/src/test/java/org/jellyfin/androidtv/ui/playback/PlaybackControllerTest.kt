package org.jellyfin.androidtv.ui.playback

import io.mockk.*
import org.jellyfin.androidtv.preference.Preference
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.KoinTest

class PlaybackControllerTest : KoinTest {
	// Mockk managed deps
	private val mockBaseItems = mockk<List<BaseItemDto>>()
	private val mockFragment = mockk<IPlaybackOverlayFragment>()

	lateinit var playbackController: PlaybackController

	// Koin managed modules
	lateinit var mockUserPreferences: UserPreferences

	private fun prepDiMocks(): Module {
		mockUserPreferences = mockk(relaxed = true)
		every {
			mockUserPreferences.get(any<Preference<PreferredVideoPlayer>>())
		} returns PreferredVideoPlayer.EXOPLAYER

		return module {
			single<UserPreferences> { mockUserPreferences }
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
			playbackController.setPlaybackSpeed(i)
			verify { mockVideoManager.setPlaybackSpeed(i) }
		}
	}

}
