package org.jellyfin.androidtv.ui.browsing.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.browsing.MediaSection
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

data class FolderBrowseUiState(
    val isLoading: Boolean = false,
    val sections: List<MediaSection> = emptyList(),
    val error: String? = null,
    val focusedItem: BaseItemDto? = null,
    val folderName: String = "",
)

class ComposeFolderViewModel : ViewModel(), KoinComponent {
    
    private val api by inject<ApiClient>()
    private val navigationRepository by inject<NavigationRepository>()
    private val imageHelper by inject<ImageHelper>()
    
    private val _uiState = MutableStateFlow(FolderBrowseUiState())
    val uiState: StateFlow<FolderBrowseUiState> = _uiState.asStateFlow()
    
    fun loadFolderContent(folder: BaseItemDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                folderName = folder.name ?: "Unknown"
            )
            
            try {
                val sections = mutableListOf<MediaSection>()
                
                // Check if this folder should show special views (like Continue Watching)
                val showSpecialViews = setOf(
                    BaseItemKind.COLLECTION_FOLDER,
                    BaseItemKind.FOLDER,
                    BaseItemKind.USER_VIEW,
                    BaseItemKind.CHANNEL_FOLDER_ITEM,
                ).contains(folder.type)
                
                if (showSpecialViews && folder.type != BaseItemKind.CHANNEL_FOLDER_ITEM) {
                    // Load "Continue Watching" section
                    val resumeItems = loadResumeItems(folder.id)
                    if (resumeItems.isNotEmpty()) {
                        sections.add(MediaSection("Continue Watching", resumeItems))
                    }
                    
                    // Load "Latest" section
                    val latestItems = loadLatestItems(folder.id)
                    if (latestItems.isNotEmpty()) {
                        sections.add(MediaSection("Latest", latestItems))
                    }
                }
                
                // Load main content ("By Name" or folder name for seasons)
                val allItems = loadAllItems(folder.id)
                if (allItems.isNotEmpty()) {
                    val sectionName = when (folder.type) {
                        BaseItemKind.SEASON -> folder.name ?: "Items"
                        else -> "By Name"
                    }
                    sections.add(MediaSection(sectionName, allItems))
                }
                
                // For seasons, also load specials
                if (folder.type == BaseItemKind.SEASON) {
                    val specialItems = loadSpecialItems(folder.id)
                    if (specialItems.isNotEmpty()) {
                        sections.add(MediaSection("Specials", specialItems))
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sections = sections
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    private suspend fun loadResumeItems(parentId: UUID): List<BaseItemDto> {
        return try {
            val request = GetItemsRequest(
                fields = ItemRepository.itemFields,
                parentId = parentId,
                limit = 50,
                filters = setOf(ItemFilter.IS_RESUMABLE),
                sortBy = setOf(ItemSortBy.DATE_PLAYED),
                sortOrder = setOf(SortOrder.DESCENDING),
            )
            val response = api.itemsApi.getItems(request = request)
            response.content.items
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun loadLatestItems(parentId: UUID): List<BaseItemDto> {
        return try {
            val request = GetItemsRequest(
                fields = ItemRepository.itemFields,
                parentId = parentId,
                limit = 50,
                filters = setOf(ItemFilter.IS_UNPLAYED),
                sortBy = setOf(ItemSortBy.DATE_CREATED),
                sortOrder = setOf(SortOrder.DESCENDING),
            )
            val response = api.itemsApi.getItems(request = request)
            response.content.items
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun loadAllItems(parentId: UUID): List<BaseItemDto> {
        return try {
            val request = GetItemsRequest(
                fields = ItemRepository.itemFields,
                parentId = parentId,
            )
            val response = api.itemsApi.getItems(request = request)
            response.content.items
        } catch (e: Exception) {
            emptyList()
        }
    }
      private suspend fun loadSpecialItems(parentId: UUID): List<BaseItemDto> {
        return try {
            // Specials are episodes with special season numbers (0, etc.)
            val request = GetItemsRequest(
                fields = ItemRepository.itemFields,
                parentId = parentId,
                includeItemTypes = setOf(BaseItemKind.EPISODE),
                // Note: This is a simplified approach - in real usage you'd need 
                // to implement the GetSpecialsRequest logic
            )
            val response = api.itemsApi.getItems(request = request)
            response.content.items.filter { 
                it.parentIndexNumber == 0 || it.indexNumber == 0 
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun onItemClick(item: BaseItemDto) {
        // Navigate to the appropriate screen based on item type
        when (item.type) {
            BaseItemKind.MOVIE, BaseItemKind.EPISODE -> {
                // Navigate to item details or start playback
                // navigationRepository.navigate(Destinations.itemDetails(item.id))
            }
            BaseItemKind.SERIES, BaseItemKind.SEASON -> {
                // Navigate to folder view for this item
                // navigationRepository.navigate(Destinations.folder(item))
            }
            BaseItemKind.FOLDER, BaseItemKind.COLLECTION_FOLDER -> {
                // Navigate to folder browse
                // navigationRepository.navigate(Destinations.folder(item))
            }
            else -> {
                // Handle other item types
            }
        }
    }
    
    fun onItemFocus(item: BaseItemDto) {
        _uiState.value = _uiState.value.copy(focusedItem = item)
    }
    
    fun getItemImageUrl(item: BaseItemDto): String? {
        return imageHelper.getPrimaryImageUrl(item)
    }
}
