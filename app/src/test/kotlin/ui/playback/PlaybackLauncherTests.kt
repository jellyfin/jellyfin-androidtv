package org.jellyfin.androidtv.ui.playback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destination
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.player.video.VideoPlayerFragment
import org.jellyfin.androidtv.util.createBundle
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaSourceType
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.MediaType
import java.util.IdentityHashMap
import java.util.Random
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

private const val AUDIO_LANGUAGE = "eng"
private const val SUBTITLE_LANGUAGE = "spa"
private const val SEEDED_POSITION = 1
private const val START_POSITION = 15_000

class PlaybackLauncherTests : FunSpec({
	test("legacy playback rejects null or empty media sources for library video") {
		listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.VIDEO).forEach { kind ->
			listOf(null, emptyList<MediaSourceInfo>()).forEach { sources ->
				withLauncher { fixture ->
					fixture.launch(listOf(libraryItem(kind, sources)))

					fixture.assertQueuePreserved()
					fixture.assertNothingStarted()
					verify(exactly = 1) {
						Toast.makeText(fixture.context, R.string.msg_cannot_play, Toast.LENGTH_LONG)
					}
					verify(exactly = 1) { fixture.toast.show() }
				}
			}
		}
	}

	test("the selected unprobed item is rejected when an earlier item could play") {
		withLauncher { fixture ->
			val items = listOf(
				libraryItem(BaseItemKind.MOVIE),
				libraryItem(BaseItemKind.EPISODE, mediaSources = null),
			)

			fixture.launch(items, itemsPosition = 1)

			fixture.assertQueuePreserved()
			fixture.assertNothingStarted()
			verify(exactly = 1) {
				Toast.makeText(fixture.context, R.string.msg_cannot_play, Toast.LENGTH_LONG)
			}
		}
	}

	test("an unprobed later item does not prevent a playable selected item from starting") {
		withLauncher { fixture ->
			val items = listOf(
				libraryItem(BaseItemKind.VIDEO, mediaSources = listOf(mediaSource(mediaStreams = emptyList()))),
				libraryItem(BaseItemKind.MOVIE, mediaSources = null),
			)

			fixture.launch(items, position = START_POSITION, replace = true)

			fixture.assertQueueReplaced(items, index = 0)
			fixture.assertLegacyPlayer(position = START_POSITION, replace = true)
			fixture.assertNoToast()
		}
	}

	test("selecting a probed item after an unprobed one still starts that item") {
		withLauncher { fixture ->
			val items = listOf(
				libraryItem(BaseItemKind.MOVIE, mediaSources = null),
				libraryItem(BaseItemKind.EPISODE),
			)

			fixture.launch(items, position = START_POSITION, replace = false, itemsPosition = 1)

			fixture.assertQueueReplaced(items, index = 1)
			fixture.assertLegacyPlayer(position = START_POSITION, replace = false)
		}
	}

	test("empty lists and out-of-range indexes do not change the playing queue") {
		withLauncher { fixture ->
			val playable = listOf(libraryItem(BaseItemKind.MOVIE), libraryItem(BaseItemKind.VIDEO))
			val launches = listOf(
				Triple(emptyList<BaseItemDto>(), 0, false),
				Triple(emptyList(), 0, true),
				Triple(playable, -1, false),
				Triple(playable, playable.size, false),
				Triple(playable, playable.size + 1, true),
				Triple(playable, Int.MIN_VALUE, false),
			)

			launches.forEach { (items, index, shuffle) ->
				fixture.launch(items, itemsPosition = index, shuffle = shuffle)
				fixture.assertQueuePreserved()
			}

			verify(exactly = launches.size) {
				Toast.makeText(fixture.context, R.string.msg_cannot_play, Toast.LENGTH_LONG)
			}
			verify(exactly = launches.size) { fixture.toast.show() }
			fixture.assertNothingStarted()
		}
	}

	test("shuffled playback validates the item at the selected index") {
		val valid = libraryItem(BaseItemKind.MOVIE)
		val unprobed = libraryItem(BaseItemKind.VIDEO, mediaSources = emptyList())
		val items = listOf(valid, unprobed)

		withLauncher(shuffleRandom = ReversingRandom()) { fixture ->
			fixture.launch(items, itemsPosition = 0, shuffle = true)

			fixture.assertQueuePreserved()
			verify(exactly = 1) {
				Toast.makeText(fixture.context, R.string.msg_cannot_play, Toast.LENGTH_LONG)
			}
		}

		withLauncher(shuffleRandom = ReversingRandom()) { fixture ->
			fixture.launch(items, position = START_POSITION, replace = true, itemsPosition = 1, shuffle = true)

			fixture.assertQueueReplaced(items.reversed(), index = 1)
			fixture.assertLegacyPlayer(position = START_POSITION, replace = true)
			fixture.assertNoToast()
		}
	}

	test("audio playback ignores absent media sources") {
		withLauncher { fixture ->
			val items = listOf(
				libraryItem(BaseItemKind.AUDIO, mediaSources = null, mediaType = MediaType.AUDIO),
				libraryItem(BaseItemKind.AUDIO, mediaSources = emptyList(), mediaType = MediaType.AUDIO),
			)

			fixture.launch(items, itemsPosition = 1, shuffle = true)

			fixture.assertQueuePreserved()
			verify(exactly = 1) { fixture.mediaManager.playNow(fixture.context, items, 1, true) }
			verify(exactly = 1) { fixture.navigationRepository.navigate(Destinations.nowPlaying) }
			verify(exactly = 0) { fixture.context.startActivity(any<Intent>()) }
			fixture.assertNoToast()
		}
	}

	test("live TV and programs keep playing when the DTO has no media sources") {
		listOf(BaseItemKind.TV_CHANNEL, BaseItemKind.PROGRAM).forEach { kind ->
			withLauncher { fixture ->
				val items = listOf(libraryItem(kind, mediaSources = null))

				fixture.launch(items, position = START_POSITION, replace = true)

				fixture.assertQueueReplaced(items, index = 0)
				fixture.assertLegacyPlayer(position = START_POSITION, replace = true)
				fixture.assertNoToast()
			}
		}
	}

	test("the external player still opens an unprobed library video") {
		withLauncher(externalPlayer = true, rewritePlayer = true) { fixture ->
			val intent = mockk<Intent>(relaxed = true)
			mockkObject(ActivityDestinations)
			try {
				every { ActivityDestinations.externalPlayer(any(), any()) } returns intent
				val items = listOf(libraryItem(BaseItemKind.MOVIE, mediaSources = null))

				fixture.launch(items, position = START_POSITION, replace = true)

				fixture.assertQueueReplaced(items, index = 0)
				verify(exactly = 1) {
					ActivityDestinations.externalPlayer(fixture.context, START_POSITION.milliseconds)
				}
				verify(exactly = 1) { fixture.context.startActivity(intent) }
				verify(exactly = 0) { fixture.navigationRepository.navigate(any<Destination>(), any()) }
				fixture.assertNoToast()
			} finally {
				unmockkObject(ActivityDestinations)
			}
		}
	}

	test("external playback is not used when a queued item cannot use it") {
		withLauncher(externalPlayer = true) { fixture ->
			val items = listOf(
				libraryItem(BaseItemKind.MOVIE, mediaSources = null),
				libraryItem(BaseItemKind.TRAILER),
			)

			fixture.launch(items)

			fixture.assertQueuePreserved()
			fixture.assertNothingStarted()
			verify(exactly = 1) {
				Toast.makeText(fixture.context, R.string.msg_cannot_play, Toast.LENGTH_LONG)
			}
		}
	}

	test("the rewrite player still opens an unprobed library video") {
		withLauncher(rewritePlayer = true) { fixture ->
			val items = listOf(libraryItem(BaseItemKind.EPISODE, mediaSources = emptyList()))

			fixture.launch(items, position = START_POSITION, replace = true)

			fixture.assertQueueReplaced(items, index = 0)
			fixture.assertRewritePlayer(position = START_POSITION, replace = true)
			verify(exactly = 0) { fixture.context.startActivity(any<Intent>()) }
			fixture.assertNoToast()
		}
	}

	test("legacy video with a source and no stream metadata replaces the queue once") {
		listOf<List<MediaStream>?>(null, emptyList()).forEach { streams ->
			withLauncher { fixture ->
				val items = listOf(
					libraryItem(BaseItemKind.VIDEO, mediaSources = listOf(mediaSource(mediaStreams = streams))),
				)

				fixture.launch(items, position = START_POSITION, replace = true)

				fixture.assertQueueReplaced(items, index = 0)
				items.single().mediaSources?.single()?.mediaStreams shouldBe streams
				fixture.assertLegacyPlayer(position = START_POSITION, replace = true)
				fixture.assertNoToast()
			}
		}
	}
})

private class LauncherFixture(
	val context: Context,
	val toast: Toast,
	val videoQueueManager: VideoQueueManager,
	val navigationRepository: NavigationRepository,
	private val session: SeededSession,
) {
	val mediaManager: MediaManager
		get() = session.mediaManager

	val launcher: PlaybackLauncher
		get() = session.launcher

	private val seededItems: List<BaseItemDto>
		get() = session.seededItems

	private val bundleValues: IdentityHashMap<Bundle, MutableMap<String, Int>>
		get() = session.bundleValues

	fun launch(
		items: List<BaseItemDto>,
		position: Int? = null,
		replace: Boolean = false,
		itemsPosition: Int = 0,
		shuffle: Boolean = false,
	) {
		launcher.launch(context, items, position, replace, itemsPosition, shuffle)
	}

	fun assertQueuePreserved() {
		videoQueueManager.getCurrentVideoQueue() shouldBe seededItems
		videoQueueManager.getCurrentMediaPosition() shouldBe SEEDED_POSITION
		videoQueueManager.getLastPlayedAudioLanguageIsoCode() shouldBe AUDIO_LANGUAGE
		videoQueueManager.getLastPlayedSubtitleLanguageIsoCode() shouldBe SUBTITLE_LANGUAGE
	}

	fun assertQueueReplaced(items: List<BaseItemDto>, index: Int) {
		videoQueueManager.getCurrentVideoQueue() shouldBe items
		videoQueueManager.getCurrentMediaPosition() shouldBe index
		videoQueueManager.getLastPlayedAudioLanguageIsoCode() shouldBe AUDIO_LANGUAGE
		videoQueueManager.getLastPlayedSubtitleLanguageIsoCode() shouldBe SUBTITLE_LANGUAGE
	}

	fun assertLegacyPlayer(position: Int?, replace: Boolean) {
		assertNavigated(CustomPlaybackOverlayFragment::class, "Position", position, replace)
	}

	fun assertRewritePlayer(position: Int?, replace: Boolean) {
		assertNavigated(VideoPlayerFragment::class, VideoPlayerFragment.EXTRA_POSITION, position, replace)
	}

	fun assertNoToast() {
		verify(exactly = 0) { Toast.makeText(any<Context>(), any<Int>(), any<Int>()) }
	}

	fun assertNothingStarted() {
		verify(exactly = 0) { navigationRepository.navigate(any<Destination>(), any()) }
		verify(exactly = 0) { navigationRepository.navigate(any<Destination>()) }
		verify(exactly = 0) { context.startActivity(any<Intent>()) }
		verify(exactly = 0) { mediaManager.playNow(any(), any(), any(), any()) }
	}

	private fun assertNavigated(
		fragment: kotlin.reflect.KClass<out androidx.fragment.app.Fragment>,
		positionKey: String,
		position: Int?,
		replace: Boolean,
	) {
		val destinationSlot = slot<Destination>()
		verify(exactly = 1) { navigationRepository.navigate(capture(destinationSlot), replace) }
		val destination = destinationSlot.captured as Destination.Fragment
		destination.fragment shouldBe fragment
		bundleValues.getValue(destination.arguments)[positionKey] shouldBe (position ?: 0)
		verify(exactly = 0) { context.startActivity(any<Intent>()) }
		verify(exactly = 0) { mediaManager.playNow(any(), any(), any(), any()) }
	}
}

private class SeededSession(
	val launcher: PlaybackLauncher,
	val mediaManager: MediaManager,
	val seededItems: List<BaseItemDto>,
	val bundleValues: IdentityHashMap<Bundle, MutableMap<String, Int>>,
)

private fun withLauncher(
	externalPlayer: Boolean = false,
	rewritePlayer: Boolean = false,
	shuffleRandom: Random = Random(),
	block: (LauncherFixture) -> Unit,
) {
	mockkStatic(Toast::class)
	mockkStatic("org.jellyfin.androidtv.util.BundleExtensionsKt")
	val toast = mockk<Toast>(relaxed = true)
	val bundleValues = IdentityHashMap<Bundle, MutableMap<String, Int>>()
	every { Toast.makeText(any<Context>(), any<Int>(), any<Int>()) } returns toast
	stubArgumentBundles(bundleValues)

	val context = mockk<Context>(relaxed = true)
	val mediaManager = mockk<MediaManager>(relaxed = true)
	val navigationRepository = mockk<NavigationRepository>(relaxed = true)
	val userPreferences = mockk<UserPreferences>()
	every { userPreferences[UserPreferences.useExternalPlayer] } returns externalPlayer
	every { userPreferences[UserPreferences.playbackRewriteVideoEnabled] } returns rewritePlayer
	val videoQueueManager = VideoQueueManager()
	val seededItems = listOf(libraryItem(BaseItemKind.MOVIE), libraryItem(BaseItemKind.EPISODE))
	videoQueueManager.setCurrentVideoQueue(seededItems)
	videoQueueManager.setCurrentMediaPosition(SEEDED_POSITION)
	videoQueueManager.setLastPlayedAudioLanguageIsoCode(AUDIO_LANGUAGE)
	videoQueueManager.setLastPlayedSubtitleLanguageIsoCode(SUBTITLE_LANGUAGE)

	try {
		block(
			LauncherFixture(
				context = context,
				toast = toast,
				videoQueueManager = videoQueueManager,
				navigationRepository = navigationRepository,
				session = SeededSession(
					launcher = PlaybackLauncher(
						mediaManager,
						videoQueueManager,
						navigationRepository,
						userPreferences,
						shuffleRandom,
					),
					mediaManager = mediaManager,
					seededItems = seededItems,
					bundleValues = bundleValues,
				),
			),
		)
	} finally {
		unmockkStatic(Toast::class)
		unmockkStatic("org.jellyfin.androidtv.util.BundleExtensionsKt")
	}
}

private fun stubArgumentBundles(bundleValues: IdentityHashMap<Bundle, MutableMap<String, Int>>) {
	every { createBundle(any()) } answers {
		@Suppress("UNCHECKED_CAST")
		val init = invocation.args.firstOrNull() as (Bundle.() -> Unit)?
		val bundle = mockk<Bundle>(relaxed = true)
		val values = mutableMapOf<String, Int>()
		bundleValues[bundle] = values
		every { bundle.putInt(any(), any()) } answers {
			values[firstArg()] = secondArg()
		}
		if (init != null) bundle.init()
		bundle
	}
}

/**
 * [java.util.Collections.shuffle] asks for an index in `0 until size`.
 * Always returning 0 swaps the tail forward, which reverses a two-item queue.
 */
private class ReversingRandom : Random() {
	override fun nextInt(bound: Int): Int = 0
}

private fun libraryItem(
	kind: BaseItemKind,
	mediaSources: List<MediaSourceInfo>? = listOf(mediaSource()),
	mediaType: MediaType = MediaType.VIDEO,
) = BaseItemDto(
	id = UUID.randomUUID(),
	type = kind,
	mediaType = mediaType,
	mediaSources = mediaSources,
)

private fun mediaSource(mediaStreams: List<MediaStream>? = null) = MediaSourceInfo(
	protocol = MediaProtocol.FILE,
	type = MediaSourceType.DEFAULT,
	isRemote = false,
	readAtNativeFramerate = false,
	ignoreDts = false,
	ignoreIndex = false,
	genPtsInput = false,
	supportsTranscoding = true,
	supportsDirectStream = true,
	supportsDirectPlay = true,
	isInfiniteStream = false,
	requiresOpening = false,
	requiresClosing = false,
	requiresLooping = false,
	supportsProbing = true,
	transcodingSubProtocol = MediaStreamProtocol.HTTP,
	hasSegments = false,
	mediaStreams = mediaStreams,
)
