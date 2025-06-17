package org.jellyfin.androidtv.ui.browsing.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jellyfin.androidtv.ui.composable.tv.MediaBrowseLayout
import org.jellyfin.androidtv.ui.theme.JellyfinTvTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Practical example: Replace EnhancedBrowseFragment with this Compose version
 * This demonstrates a working migration that you can test immediately
 */
class ComposeEnhancedBrowseFragment : Fragment() {
    
    private val browseViewModel: ComposeBrowseViewModel by viewModel()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                JellyfinTvTheme {
                    val uiState by browseViewModel.uiState.collectAsState()
                    
                    MediaBrowseLayout(
                        sections = uiState.sections,
                        onItemClick = { item ->
                            browseViewModel.onItemClick(item)
                        },
                        onItemFocus = { item ->
                            browseViewModel.onItemFocus(item)
                        },
                        getItemImageUrl = { item ->
                            browseViewModel.getItemImageUrl(item)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp)
                    )
                }
            }
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Load data when fragment is created
        browseViewModel.loadBrowseData(arguments)
    }
    
    companion object {
        fun newInstance(args: Bundle?): ComposeEnhancedBrowseFragment {
            return ComposeEnhancedBrowseFragment().apply {
                arguments = args
            }
        }
    }
}
