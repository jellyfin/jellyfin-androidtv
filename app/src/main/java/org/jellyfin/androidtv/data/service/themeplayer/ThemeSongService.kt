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
 *
 * - When an item page is requested, cancels any previous delayed playback stop requests.
 * - When an item page is displayed, fetches and caches the theme song, then passes it to [ThemeSongPlayer].
 * - When an item page is closed, first ensures we're not waiting for a item page to be displayed. If not, schedules a delayed playback
 * stop job. If a new item details opens in the meanwhile, the stop request is cancelled.
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
		REQUESTED, // Item page was requested to launch, but it hasn't been displayed yet.
		DISPLAYED, // Item page is being displayed.
		HIDDEN // Item page was closed.
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
		// Cancel any previously scheduled stops. If needed, we will stop when [itemPageDisplayed] is called.
		// This is mainly to continue playing when navigating between items with same theme songs.
		delayedThemeSongStopJob?.cancel()
	}

	/**
	 * Called when a new item page is displayed.
	 */
	fun itemPageDisplayed(baseItem: BaseItemDto?) {
		itemPageState = ItemPageState.DISPLAYED
		// Cancel any previously scheduled stops. If needed, we will stop further down.
		// This is mainly to continue playing when navigating between items with same theme songs.
		delayedThemeSongStopJob?.cancel()
		val currentUserId = userRepository.currentUser.value?.id
		if (currentUserId == null || !userPreferences[UserPreferences.themeSongsEnabled]) return themeSongPlayer.stopThemeSong()

		if (baseItem?.type == BaseItemKind.PERSON) {
			// No need to interrupt the playback if a person page is showed.
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
			// [itemPageHidden] was called because a newly requested item page is going to be displayed on top.
			// Don't take any action now. We'll decide what to do when the new item page is displayed.
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
