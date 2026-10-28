package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests

class PlaybackAudioTest : FunSpec({
	lateinit var playback: PlaybackFixture

	val transitions = mapOf<String, suspend (PlaybackFixture) -> Unit>(
		"Force skip to the next episode with %s" to PlaybackFixture::skip,
		"Finish the episode and autoplay the next with %s" to PlaybackFixture::next,
	)

	beforeTest {
		playback = PlaybackFixture()
		playback.initialize()
	}

	afterTest {
		playback.deinitialize()
	}

	test("Select non-default audio with a different language") {
		val dubbed = audio(language = "eng", isDefault = true)
		val origin = audio(language = "jpn")

		playback.play(media(video(), dubbed, origin))
		playback.selectAudio(origin)
		playback.refresh()
		playback.assertAudio(origin)
	}

	test("Select non-default audio with the same language") {
		val voice = audio(language = "jpn", isDefault = true)
		val comment = audio(language = "jpn")

		playback.play(media(video(), voice, comment))
		playback.selectAudio(comment)
		playback.refresh()
		playback.assertAudio(comment)
	}

	test("Fallback to existing audio when the selected index is lost") {
		val selected = audio()
		val fallback = audio()

		playback.play(media(video(), audio(), selected), media(video(), fallback, subtitle()))
		playback.selectAudio(selected)
		playback.skip()
		playback.assertAudio(fallback)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("non-default audio in a different language")
	}) { method ->
		val fstDubbed = audio(language = "eng", isDefault = true)
		val fstOrigin = audio(language = "jpn")
		val sndDubbed = fstDubbed.copy()
		val sndOrigin = fstOrigin.copy()

		playback.play(media(video(), fstDubbed, fstOrigin), media(video(), sndDubbed, sndOrigin))
		playback.selectAudio(fstOrigin)
		method(playback)
		playback.assertAudio(sndOrigin)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("non-default audio of the same language")
	}) { method ->
		val fstVoice = audio(language = "jpn", isDefault = true)
		val fstComment = audio(language = "jpn")
		val sndVoice = fstVoice.copy()
		val sndComment = fstComment.copy()

		playback.play(media(video(), fstVoice, fstComment), media(video(), sndVoice, sndComment))
		playback.selectAudio(fstComment)
		method(playback)
		playback.assertAudio(sndComment)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("reordered audio in a different language")
	}) { method ->
		val fstDubbed = audio(language = "eng")
		val fstOrigin = audio(language = "jpn")
		val sndDubbed = fstDubbed.copy()
		val sndOrigin = fstOrigin.copy()

		playback.play(media(video(), fstDubbed, fstOrigin), media(video(), sndOrigin, sndDubbed))
		playback.selectAudio(fstDubbed)
		method(playback)
		playback.assertAudio(sndDubbed)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("reordered audio of the same language")
	}) { method ->
		val fstVoice = audio(language = "jpn", title = "Voice")
		val fstComment = audio(language = "jpn", title = "Comment")
		val sndVoice = fstVoice.copy()
		val sndComment = fstComment.copy()

		playback.play(media(video(), fstVoice, fstComment), media(video(), sndComment, sndVoice))
		playback.selectAudio(fstVoice)
		method(playback)
		playback.assertAudio(sndVoice)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("misspelled audio of the same language")
	}) { method ->
		val fstVoice = audio(language = "jpn", title = "Voice")
		val fstComment = audio(language = "jpn", title = "Comment")
		val sndVoice = fstVoice.copy()
		val sndComment = audio(language = "jpn", title = "Coment")

		playback.play(media(video(), fstVoice, fstComment), media(video(), sndVoice, sndComment))
		playback.selectAudio(fstComment)
		method(playback)
		playback.assertAudio(sndComment)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("unavailable selected language")
	}) { method ->
		val fstOrigin = audio(language = "jpn", isDefault = true)
		val fstDubbed = audio(language = "eng")
		val sndOrigin = fstOrigin.copy()
		val sndDubbed = fstDubbed.copy(language = "rus")

		playback.play(media(video(), fstOrigin, fstDubbed), media(video(), sndOrigin, sndDubbed))
		playback.selectAudio(fstDubbed)
		method(playback)
		playback.assertAudio(sndOrigin)
	}
})
