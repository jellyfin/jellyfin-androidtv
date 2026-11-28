package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod

class PlaybackTranscodeTest : FunSpec({
	lateinit var playback: PlaybackFixture

	beforeTest {
		playback = PlaybackFixture(PlayMethod.TRANSCODE)
		playback.initialize()
	}

	afterTest {
		playback.deinitialize()
	}

	test("Skip restores the selected burned subtitle language") {
		val restored = subtitle(language = "eng").copy(deliveryMethod = SubtitleDeliveryMethod.ENCODE)

		playback.play(
			media(video(), audio(), subtitle(language = "eng", isDefault = true)),
			media(video(), audio(), restored, restored.copy(language = "jpn", isDefault = true)),
		)
		playback.skip()
		playback.assertSubtitle(restored)
	}

	test("Skip restores the selected transcoded audio language") {
		val restored = audio(language = "eng")

		playback.play(
			media(video(), audio(language = "eng", isDefault = true)),
			media(video(), audio(language = "jpn", isDefault = true), restored),
		)
		playback.skip()
		playback.assertAudio(restored)
	}
})
