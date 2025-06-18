package org.jellyfin.androidtv.ui.browsing.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.ui.theme.JellyfinTvTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Fragment that hosts the Compose Movies screen
 * Replaces BrowseGridFragment for Movies library browsing
 */
class ComposeMoviesFragment : Fragment() {
    
    private val moviesViewModel: ComposeMoviesViewModel by viewModel()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                JellyfinTvTheme {
                    ComposeMoviesScreen(
                        folderArguments = arguments,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
    
    companion object {
        fun newInstance(args: Bundle?): ComposeMoviesFragment {
            return ComposeMoviesFragment().apply {
                arguments = args
            }
        }
    }
}
