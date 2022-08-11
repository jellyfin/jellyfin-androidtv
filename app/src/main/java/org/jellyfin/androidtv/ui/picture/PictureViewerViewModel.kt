package org.jellyfin.androidtv.ui.picture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class PictureViewerViewModel(private val api: ApiClient) : ViewModel() {
	private var album: List<BaseItemDto> = emptyList()
	private var albumIndex = -1

	private val _currentItem = MutableStateFlow<BaseItemDto?>(null)
	val currentItem = _currentItem.asStateFlow()

	suspend fun loadItem(id: UUID) {
		// Load requested item
		val itemResponse by api.userLibraryApi.getItem(itemId = id)
		_currentItem.value = itemResponse

		val albumResponse by api.itemsApi.getItemsByUserId(
			parentId = itemResponse.parentId,
			includeItemTypes = setOf(BaseItemKind.PHOTO),
			fields = setOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO),
			sortBy = listOf(ItemFields.SORT_NAME.name), // TODO: Order should be consistent with the stdgridview the user comes from, which allows to change the order
			sortOrder = listOf(SortOrder.ASCENDING),
		)
		album = albumResponse.items.orEmpty()
		albumIndex = album.indexOfFirst { it.id == id }
	}

	// Album actions

	fun showNext() {
		albumIndex++
		if (albumIndex == album.size) albumIndex = 0

		_currentItem.value = album[albumIndex]
		restartPresentation()
	}

	fun showPrevious() {
		albumIndex--
		if (albumIndex == -1) albumIndex = album.size - 1

		_currentItem.value = album[albumIndex]
		restartPresentation()
	}

	// Presentation

	private var presentationJob: Job? = null
	private val _presentationActive = MutableStateFlow(false)
	val presentationActive = _presentationActive.asStateFlow()
	var presentationDelay = 8.seconds

	fun createPresentationJob() = viewModelScope.launch(Dispatchers.IO) {
		while (isActive) {
			delay(presentationDelay)
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
