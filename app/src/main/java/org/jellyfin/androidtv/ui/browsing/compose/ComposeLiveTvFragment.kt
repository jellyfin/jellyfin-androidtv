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
 * Fragment that hosts the Compose Live TV screen
 * Replaces BrowseViewFragment for Live TV library browsing
 */
class ComposeLiveTvFragment : Fragment() {

	private val liveTvViewModel: ComposeLiveTvViewModel by viewModel()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		return ComposeView(requireContext()).apply {
			setContent {
				JellyfinTvTheme {
					ComposeLiveTvScreen(
						folderArguments = arguments,
						modifier = Modifier.fillMaxSize(),
					)
				}
			}
		}
	}

	companion object {
		fun newInstance(args: Bundle?): ComposeLiveTvFragment {
			return ComposeLiveTvFragment().apply {
				arguments = args
			}
		}
	}
}
