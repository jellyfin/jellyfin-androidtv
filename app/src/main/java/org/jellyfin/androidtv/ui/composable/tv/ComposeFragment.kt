package org.jellyfin.androidtv.ui.composable.tv

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import org.jellyfin.androidtv.ui.theme.JellyfinTvTheme

/**
 * Bridge fragment to help migrate from Leanback fragments to Compose
 * This allows gradual migration by wrapping Compose content in a Fragment
 */
abstract class ComposeFragment : Fragment() {
    
    @Composable
    abstract fun Content()
    
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            JellyfinTvTheme {
                Content()
            }
        }
    }
}

/**
 * Example migration of a browse fragment
 */
class ComposeBrowseFragment : ComposeFragment() {
    
    @Composable
    override fun Content() {
        // This would contain your migrated Compose content
        // For example, replacing EnhancedBrowseFragment with MediaBrowseLayout
        
        /*
        val sections = remember { loadSections() }
        MediaBrowseLayout(
            sections = sections,
            onItemClick = { item -> 
                // Handle navigation
            },
            onItemFocus = { item ->
                // Handle focus changes
            }
        )
        */
    }
}

/**
 * Helper extension to add Compose content to existing fragments
 */
fun Fragment.setComposeContent(content: @Composable () -> Unit) {
    val composeView = ComposeView(requireContext())
    composeView.setContent {
        JellyfinTvTheme {
            content()
        }
    }
    // You would need to add this to your fragment's layout
}

/**
 * Interop helper for mixing Leanback and Compose during migration
 */
@Composable
fun LeanbackToComposeInterop(
    leanbackContent: @Composable () -> Unit,
    composeContent: @Composable () -> Unit,
    useCompose: Boolean = false
) {
    if (useCompose) {
        composeContent()
    } else {
        leanbackContent()
    }
}
