package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import java.util.UUID

class PlaybackSubtitleTest : FunSpec({
	lateinit var playback: PlaybackFixture
	val seriesId = UUID.randomUUID()

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

	test("Check that default subtitles are enabled") {
		val default = subtitle(language = "eng", isDefault = true)
		playback.play(media(video(), audio(), default))
		playback.assertSubtitle(default)
	}

	test("Check that subtitles without flags are disabled by default") {
		playback.play(media(video(), audio(), subtitle()))
		playback.assertSubtitle(null)
	}

	test("Check that external subtitles also disabled by default") {
		playback.play(media(video(), audio(), subtitle(isExternal = true)))
		playback.assertSubtitle(null)
	}

	test("Disable default subtitles") {
		playback.play(media(video(), audio(), subtitle(isDefault = true)))
		playback.disableSubtitles()
		playback.refresh()
		playback.assertSubtitle(null)
	}

	test("Disable forced default subtitles") {
		playback.play(media(video(), audio(), subtitle(isDefault = true, isForced = true)))
		playback.disableSubtitles()
		playback.refresh()
		playback.assertSubtitle(null)
	}

	test("Select external subtitles") {
		val external = subtitle(language = "eng", isExternal = true)

		playback.play(media(video(), audio(), external))
		playback.selectSubtitle(external)
		playback.refresh()
		playback.assertSubtitle(external)
	}

	test("Select non-default subtitles with a different language") {
		val default = subtitle(language = "eng", isDefault = true)
		val origin = subtitle(language = "jpn")

		playback.play(media(video(), audio(), default, origin))
		playback.selectSubtitle(origin)
		playback.refresh()
		playback.assertSubtitle(origin)
	}

	test("Select non-default subtitles with the same language") {
		val song = subtitle(language = "eng", isDefault = true)
		val full = subtitle(language = "eng")

		playback.play(media(video(), audio(), song, full))
		playback.selectSubtitle(full)
		playback.refresh()
		playback.assertSubtitle(full)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("disabled default subtitles")
	}) { method ->
		playback.play(
			media(video(), audio(), subtitle(isDefault = true)).copy(seriesId = seriesId),
			media(video(), audio(), subtitle(isDefault = true)).copy(seriesId = seriesId),
		)
		playback.disableSubtitles()
		method(playback)
		playback.assertSubtitle(null)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("disabled external default subtitles")
	}) { method ->
		playback.play(
			media(video(), audio(), subtitle(isDefault = true, isExternal = true)).copy(seriesId = seriesId),
			media(video(), audio(), subtitle(isDefault = true, isExternal = true)).copy(seriesId = seriesId),
		)
		playback.disableSubtitles()
		method(playback)
		playback.assertSubtitle(null)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("non-default subtitles in a different language")
	}) { method ->
		val fstDefault = subtitle(language = "eng", isDefault = true)
		val fstOrigin = subtitle(language = "jpn")
		val sndDefault = fstDefault.copy()
		val sndOrigin = fstOrigin.copy()

		playback.play(
			media(video(), audio(), fstDefault, fstOrigin),
			media(video(), audio(), sndDefault, sndOrigin),
		)
		playback.selectSubtitle(fstOrigin)
		method(playback)
		playback.assertSubtitle(sndOrigin)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("non-default subtitles of the same language")
	}) { method ->
		val fstSong = subtitle(language = "jpn", isDefault = true)
		val fstFull = subtitle(language = "jpn")
		val sndSong = fstSong.copy()
		val sndFull = fstFull.copy()

		playback.play(
			media(video(), audio(), fstSong, fstFull).copy(seriesId = seriesId),
			media(video(), audio(), sndSong, sndFull).copy(seriesId = seriesId),
		)
		playback.selectSubtitle(fstFull)
		method(playback)
		playback.assertSubtitle(sndFull)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("reordered subtitles in a different language")
	}) { method ->
		val fstDefault = subtitle(language = "eng")
		val fstOrigin = subtitle(language = "jpn")
		val sndDefault = fstDefault.copy()
		val sndOrigin = fstOrigin.copy()

		playback.play(
			media(video(), audio(), fstDefault, fstOrigin),
			media(video(), audio(), sndOrigin, sndDefault),
		)
		playback.selectSubtitle(fstDefault)
		method(playback)
		playback.assertSubtitle(sndDefault)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("reordered subtitles of the same language")
	}) { method ->
		val fstSong = subtitle(language = "jpn", title = "Song")
		val fstFull = subtitle(language = "jpn", title = "Full")
		val sndSong = fstSong.copy()
		val sndFull = fstFull.copy()

		playback.play(
			media(video(), audio(), fstSong, fstFull).copy(seriesId = seriesId),
			media(video(), audio(), sndFull, sndSong).copy(seriesId = seriesId),
		)
		playback.selectSubtitle(fstSong)
		method(playback)
		playback.assertSubtitle(sndSong)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("misspelled subtitles of the same language")
	}) { method ->
		val fstSong = subtitle(language = "jpn", title = "Song")
		val fstFull = subtitle(language = "jpn", title = "Full")
		val sndSong = fstSong.copy()
		val sndFull = subtitle(language = "jpn", title = "Fall")

		playback.play(
			media(video(), audio(), fstSong, fstFull).copy(seriesId = seriesId),
			media(video(), audio(), sndSong, sndFull).copy(seriesId = seriesId),
		)
		playback.selectSubtitle(fstFull)
		method(playback)
		playback.assertSubtitle(sndFull)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("unavailable selected language")
	}) { method ->
		val fstOrigin = subtitle(language = "jpn", isDefault = true)
		val fstTranslated = subtitle(language = "eng")
		val sndOrigin = fstOrigin.copy()
		val sndTranslated = fstTranslated.copy(language = "rus")

		playback.play(
			media(video(), audio(), fstOrigin, fstTranslated),
			media(video(), audio(), sndOrigin, sndTranslated),
		)
		playback.selectSubtitle(fstTranslated)
		method(playback)
		playback.assertSubtitle(sndOrigin)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("short to long title changes and reordering of the same language")
	}) { method ->
		val official = subtitle(language = "eng", title = "Official", isDefault = true)
		val comments = subtitle(language = "eng", title = "Comments")
		val fansub = subtitle(language = "eng", title = "Fansub")
		val person = subtitle(language = "eng", title = "John Doe [Fansub]")

		playback.play(
			media(video(), audio(), official, comments, fansub).copy(seriesId = seriesId),
			media(video(), audio(), official.copy(), person, comments.copy()).copy(seriesId = seriesId),
		)
		playback.selectSubtitle(fansub)
		method(playback)
		playback.assertSubtitle(person)
	}

	withTests(transitions.mapKeys { (template, _) ->
		template.format("long to short title changes and reordering of the same language")
	}) { method ->
		val official = subtitle(language = "eng", title = "Official", isDefault = true)
		val comments = subtitle(language = "eng", title = "Comments")
		val fansub = subtitle(language = "eng", title = "Fansub")
		val person = subtitle(language = "eng", title = "John Doe [Fansub]")

		playback.play(
			media(video(), audio(), official, comments, person).copy(seriesId = seriesId),
			media(video(), audio(), official.copy(), fansub, comments.copy()).copy(seriesId = seriesId),
		)
		playback.selectSubtitle(person)
		method(playback)
		playback.assertSubtitle(fansub)
	}
})
