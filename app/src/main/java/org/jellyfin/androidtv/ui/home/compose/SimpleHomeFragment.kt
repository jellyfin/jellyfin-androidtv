package org.jellyfin.androidtv.ui.home.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.ui.theme.JellyfinTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Simple Compose-based Fragment for testing the home screen migration.
 * This is a minimal example to demonstrate the gradual migration approach.
 */
class SimpleHomeFragment : Fragment() {

	private val viewModel by viewModel<SimpleHomeViewModel>()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		android.util.Log.d("SimpleHomeFragment", "onCreateView() called - Compose home screen loading")
		return ComposeView(requireContext()).apply {
			setContent {
				JellyfinTheme {
					val homeState by viewModel.homeState.collectAsState()

					SimpleHomeScreen(
						homeState = homeState,
						viewModel = viewModel,
						onLibraryClick = viewModel::onLibraryClick,
						onItemClick = viewModel::onItemClick,
						getItemImageUrl = viewModel::getItemImageUrl,
						modifier = Modifier.fillMaxSize(),
					)
				}
			}
		}
	}
}
