package org.jellyfin.androidtv.ui.browsing.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.ui.composable.tv.MediaSection
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for the Compose version of home browsing
 * Demonstrates how to adapt existing repository patterns for Compose
 */
class HomeBrowseViewModel : ViewModel(), KoinComponent {
    
    private val userViewsRepository by inject<UserViewsRepository>()
    private val itemRepository by inject<ItemRepository>()
    private val navigationRepository by inject<NavigationRepository>()
    private val imageHelper by inject<ImageHelper>()
    
    private val _uiState = MutableStateFlow(HomeBrowseUiState())
    val uiState: StateFlow<HomeBrowseUiState> = _uiState.asStateFlow()
    
    init {
        loadHomeContent()
    }
    
    private fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val sections = mutableListOf<MediaSection>()
                
                // Load "Continue Watching" section
                val resumeItems = loadResumeItems()
                if (resumeItems.isNotEmpty()) {
                    sections.add(MediaSection("Continue Watching", resumeItems))
                }
                
                // Load "Next Up" section  
                val nextUpItems = loadNextUpItems()
                if (nextUpItems.isNotEmpty()) {
                    sections.add(MediaSection("Next Up", nextUpItems))
                }
                
                // Load "Latest" sections per library
                val libraryLatestSections = loadLatestByLibrary()
                sections.addAll(libraryLatestSections)
                
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
    
    private suspend fun loadResumeItems(): List<BaseItemDto> {
        // TODO: Implement resume items loading
        return emptyList()
    }
    
    private suspend fun loadNextUpItems(): List<BaseItemDto> {
        // TODO: Implement next up items loading
        return emptyList()
    }
    
    private suspend fun loadLatestByLibrary(): List<MediaSection> {
        // TODO: Implement latest items by library
        return emptyList()
    }
    
    fun onItemClick(item: BaseItemDto, navController: NavHostController) {
        // Handle item click navigation
        when (item.type) {
            org.jellyfin.sdk.model.api.BaseItemKind.MOVIE,
            org.jellyfin.sdk.model.api.BaseItemKind.EPISODE -> {
                navigationRepository.navigate(Destinations.itemDetails(item.id))
            }
            org.jellyfin.sdk.model.api.BaseItemKind.SERIES -> {
                navigationRepository.navigate(Destinations.itemDetails(item.id))
            }
            org.jellyfin.sdk.model.api.BaseItemKind.COLLECTION_FOLDER -> {
                navigationRepository.navigate(Destinations.libraryBrowser(item))
            }
            else -> {
                navigationRepository.navigate(Destinations.itemDetails(item.id))
            }
        }
    }
    
    fun onItemFocus(item: BaseItemDto) {
        _uiState.value = _uiState.value.copy(focusedItem = item)
        
        // TODO: Update background image based on focused item
        // backgroundService.setBackground(item)
    }
    
    fun getItemImageUrl(item: BaseItemDto): String? {
        return imageHelper.getPrimaryImageUrl(item, null, 300)
    }
    
    fun refreshContent() {
        loadHomeContent()
    }
}
