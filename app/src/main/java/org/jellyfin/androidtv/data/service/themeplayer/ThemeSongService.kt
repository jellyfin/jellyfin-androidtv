package org.jellyfin.androidtv.data.service.themeplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.AudioEventListener
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles theme song playing on notification from navigation in various item pages.
 */
class ThemeSongService(
	private val api: ApiClient,
	private val userPreferences: UserPreferences,
	private val userRepository: UserRepository,
	private val themeSongPlayer: ThemeSongPlayer,
) : AudioEventListener {

	/**
	 * Keeps track of the state of the current item page.
	 */
	enum class ItemPageState {
		REQUESTED,
		DISPLAYED, 
		HIDDEN 
	}

	private val scope = MainScope()
	private val itemToThemeSongCache = mutableMapOf<UUID, BaseItemDto>()
	private val delayedStopDuration = 200.milliseconds
	private var delayedThemeSongStopJob: Job? = null
	private var itemPageState: ItemPageState = ItemPageState.HIDDEN

	/**
	 * Called when a new item page was requested and is about to be displayed.
	 */
	fun itemPageRequested() {
		itemPageState = ItemPageState.REQUESTED
		delayedThemeSongStopJob?.cancel()
	}

	/**
	 * Called when a new item page is displayed.
	 */
	fun itemPageDisplayed(baseItem: BaseItemDto?) {
		itemPageState = ItemPageState.DISPLAYED
		delayedThemeSongStopJob?.cancel()
		val currentUserId = userRepository.currentUser.value?.id
		if (currentUserId == null || !userPreferences[UserPreferences.themeSongsEnabled]) return themeSongPlayer.stopThemeSong()

		if (baseItem?.type == BaseItemKind.PERSON) {
			return
		}

		if (baseItem == null) return themeSongPlayer.stopThemeSong()

		themeSongPlayer.prepareForPlay()
		scope.launch(Dispatchers.IO) {
			loadAndPlayThemeSong(baseItem.id, currentUserId)
		}
	}

	/**
	 * Called when an item page is getting hidden, either by a closing or another page showing on top.
	 */
	fun itemPageHidden() {
		if (itemPageState == ItemPageState.REQUESTED) {
			return
		}
		itemPageState = ItemPageState.HIDDEN
		delayedThemeSongStopJob?.cancel()
		delayedThemeSongStopJob = scope.launch(Dispatchers.Main) {
			// Don't immediately stop the theme song in case we navigated to another item page with the same song.
			delay(delayedStopDuration)
			themeSongPlayer.stopThemeSong()
		}
	}

	private suspend fun loadAndPlayThemeSong(itemId: UUID, userId: UUID) {
		if (!itemToThemeSongCache.contains(itemId)) {
			val loadedThemeSong = api.libraryApi.getThemeSongs(itemId, userId, true).content.items.firstOrNull()
			if (loadedThemeSong != null) itemToThemeSongCache[itemId] = loadedThemeSong
		}
		scope.launch(Dispatchers.Main) { themeSongPlayer.playThemeSong(itemToThemeSongCache[itemId]) }
	}
}
