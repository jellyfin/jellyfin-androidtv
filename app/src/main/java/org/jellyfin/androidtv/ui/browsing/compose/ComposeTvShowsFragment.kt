package org.jellyfin.androidtv.ui.browsing.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.ui.theme.JellyfinTvTheme

/**
 * Fragment that hosts the Compose TV Shows screen
 */
class ComposeTvShowsFragment : Fragment() {

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return ComposeView(requireContext()).apply {
			setContent {
				JellyfinTvTheme {
					ComposeTvShowsScreen(
						folderArguments = arguments
					)
				}
			}
		}
	}

	companion object {
		fun newInstance(args: Bundle?): ComposeTvShowsFragment {
			return ComposeTvShowsFragment().apply {
				arguments = args
			}
		}
	}
}
