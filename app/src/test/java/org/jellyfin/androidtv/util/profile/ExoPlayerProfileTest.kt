package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.CodecTypes
import org.junit.Test
import org.junit.Assert.assertTrue

class ExoPlayerProfileTest {
	@Test
	fun testDownmixAudioCodecs() {
		val codecs = ExoPlayerProfile.getDownmixSupportedAudioCodecs()

		arrayOf(CodecTypes.AAC, CodecTypes.MP2, CodecTypes.MP3).forEach { expected ->
			assertTrue(codecs.contains(expected))
		}
	}

	@Test
	fun testAllAudioCodecsContainsDownMixCodecs() {
		val codecs = ExoPlayerProfile.getAllSupportedAudioCodecs()

		arrayOf(CodecTypes.AAC, CodecTypes.MP2, CodecTypes.MP3).forEach { expected ->
			assertTrue(codecs.contains(expected))
		}
	}

	@Test
	fun testAllAudioCodecs() {
		val codecs = ExoPlayerProfile.getAllSupportedAudioCodecs()

		// Check a random sub-sample
		arrayOf(CodecTypes.AAC_LATM, CodecTypes.MLP, CodecTypes.PCM_ALAW).forEach { expected ->
			assertTrue(codecs.contains(expected))
		}
	}

}
