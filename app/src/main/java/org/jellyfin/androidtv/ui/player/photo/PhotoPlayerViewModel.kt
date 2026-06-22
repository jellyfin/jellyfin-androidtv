package org.jellyfin.androidtv.ui.player.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class PhotoPlayerViewModel(
	private val api: ApiClient,
	private val systemPreferences: SystemPreferences,
) : ViewModel() {
	private var originalAlbum: List<BaseItemDto> = emptyList()
	private var currentAlbum: List<BaseItemDto> = emptyList()
	private var albumIndex = -1

	private val _currentItem = MutableStateFlow<BaseItemDto?>(null)
	val currentItem = _currentItem.asStateFlow()

	suspend fun loadItem(id: UUID, sortBy: Collection<ItemSortBy>, sortOrder: SortOrder) {
		// Load requested item
		val itemResponse = withContext(Dispatchers.IO) {
			api.userLibraryApi.getItem(itemId = id).content
		}
		_currentItem.value = itemResponse

		val albumResponse = withContext(Dispatchers.IO) {
			api.itemsApi.getItems(
				parentId = itemResponse.parentId,
				includeItemTypes = setOf(BaseItemKind.PHOTO),
				fields = ItemRepository.itemFields,
				sortBy = sortBy,
				sortOrder = listOf(sortOrder),
			).content
		}
		originalAlbum = albumResponse.items

		// In some rare cases the album of the image might be empty when the
		// files are considered invalid by the server
		if (originalAlbum.isEmpty()) {
			originalAlbum = listOf(itemResponse)
		}

		currentAlbum = if (shuffleActive.value) originalAlbum.shuffled() else originalAlbum
		albumIndex = currentAlbum.indexOfFirst { it.id == id }
	}

	// Album actions

	fun toggleShuffle() {
		shuffleActive.value = !shuffleActive.value
		val current = _currentItem.value
		currentAlbum = if (shuffleActive.value) originalAlbum.shuffled() else originalAlbum
		if (current != null) {
			albumIndex = currentAlbum.indexOfFirst { it.id == current.id }
		}
	}

	fun showNext() {
		if (currentAlbum.isEmpty()) return

		albumIndex++
		if (albumIndex == currentAlbum.size) albumIndex = 0

		_currentItem.value = currentAlbum[albumIndex]
		restartPresentation()
	}

	fun showPrevious() {
		if (currentAlbum.isEmpty()) return

		albumIndex--
		if (albumIndex == -1) albumIndex = currentAlbum.size - 1

		_currentItem.value = currentAlbum[albumIndex]
		restartPresentation()
	}

	// Presentation

	val shuffleActive = MutableStateFlow(false)

	private var presentationJob: Job? = null
	private val _presentationActive = MutableStateFlow(false)
	val presentationActive = _presentationActive.asStateFlow()
	
	val presentationDelay = MutableStateFlow(systemPreferences[SystemPreferences.photoPlayerInterval].seconds)

	fun cycleInterval() {
		val intervals = listOf(3, 5, 8, 10, 15, 30, 60)
		val currentSeconds = presentationDelay.value.inWholeSeconds.toInt()
		val currentIndex = intervals.indexOf(currentSeconds).takeIf { it >= 0 } ?: 2
		val nextSeconds = intervals[(currentIndex + 1) % intervals.size]

		presentationDelay.value = nextSeconds.seconds
		systemPreferences[SystemPreferences.photoPlayerInterval] = nextSeconds

		restartPresentation()
	}

	fun createPresentationJob() = viewModelScope.launch(Dispatchers.IO) {
		while (isActive) {
			delay(presentationDelay.value)
			showNext()
		}
	}

	fun startPresentation() {
		if (presentationActive.value) return
		_presentationActive.value = true

		presentationJob = createPresentationJob()
	}

	fun restartPresentation() {
		if (!presentationActive.value) return

		presentationJob?.cancel()
		presentationJob = createPresentationJob()
	}

	fun stopPresentation() {
		if (!presentationActive.value) return

		presentationJob?.cancel()
		presentationJob = null
		_presentationActive.value = false
	}

	fun togglePresentation() {
		if (presentationActive.value) stopPresentation()
		else startPresentation()
	}
}
