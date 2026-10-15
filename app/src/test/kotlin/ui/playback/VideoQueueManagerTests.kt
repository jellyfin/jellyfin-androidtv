package org.jellyfin.androidtv.ui.playback

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.auth.repository.UserRepositoryImpl
import org.jellyfin.sdk.model.api.SubtitlePlaybackMode
import org.jellyfin.sdk.model.api.UserConfiguration
import org.jellyfin.sdk.model.api.UserDto
import java.util.UUID

class VideoQueueManagerTests : FunSpec({
	fun user(rememberAudioSelections: Boolean) = UserDto(
		id = UUID.randomUUID(),
		hasPassword = false,
		hasConfiguredPassword = false,
		hasConfiguredEasyPassword = false,
		configuration = UserConfiguration(
			audioLanguagePreference = "rus",
			playDefaultAudioTrack = false,
			displayMissingEpisodes = false,
			groupedFolders = emptyList(),
			subtitleMode = SubtitlePlaybackMode.DEFAULT,
			displayCollectionsView = false,
			enableLocalPassword = false,
			orderedViews = emptyList(),
			latestItemsExcludes = emptyList(),
			myMediaExcludes = emptyList(),
			hidePlayedInLatest = false,
			rememberAudioSelections = rememberAudioSelections,
			rememberSubtitleSelections = false,
			enableNextEpisodeAutoPlay = true,
		),
	)

	fun userRepository(rememberAudioSelections: Boolean) = UserRepositoryImpl().apply {
		setCurrentUser(user(rememberAudioSelections))
	}

	test("disabled audio remembering does not force a previously selected language") {
		val queue = VideoQueueManager(userRepository(rememberAudioSelections = false))
		queue.setLastPlayedAudioLanguageIsoCode("eng")

		queue.getLastPlayedAudioLanguageIsoCode() shouldBe null
	}

	test("automatic fallback does not become a remembered language") {
		val queue = VideoQueueManager(userRepository(rememberAudioSelections = true))
		queue.setLastPlayedAudioLanguageIsoCode("eng", selectedByUser = false)

		queue.getLastPlayedAudioLanguageIsoCode() shouldBe null
	}

	test("automatic fallback preserves the explicit language selection") {
		val queue = VideoQueueManager(userRepository(rememberAudioSelections = true))
		queue.setLastPlayedAudioLanguageIsoCode("rus")
		queue.setLastPlayedAudioLanguageIsoCode("eng", selectedByUser = false)

		queue.getLastPlayedAudioLanguageIsoCode() shouldBe "rus"
	}

	test("disabling audio remembering takes effect for the existing queue") {
		val users = userRepository(rememberAudioSelections = true)
		val queue = VideoQueueManager(users)
		queue.setLastPlayedAudioLanguageIsoCode("eng")
		queue.getLastPlayedAudioLanguageIsoCode() shouldBe "eng"

		val currentUser = requireNotNull(users.currentUser.value)
		users.setCurrentUser(currentUser.copy(
			configuration = requireNotNull(currentUser.configuration).copy(rememberAudioSelections = false),
		))

		queue.getLastPlayedAudioLanguageIsoCode() shouldBe null
	}

	test("missing user does not force a remembered language") {
		val queue = VideoQueueManager(UserRepositoryImpl())
		queue.setLastPlayedAudioLanguageIsoCode("eng")

		queue.getLastPlayedAudioLanguageIsoCode() shouldBe null
	}
})
