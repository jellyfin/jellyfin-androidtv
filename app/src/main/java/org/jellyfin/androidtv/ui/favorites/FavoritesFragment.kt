package org.jellyfin.androidtv.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.leanback.app.RowsSupportFragment
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class FavoritesFragment : Fragment() {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		val viewModel = koinViewModel<FavoritesViewModel>()
		val favoritesFragmentDelegate = koinInject<FavoritesFragmentDelegate> { parametersOf(requireContext()) }

		LaunchedEffect(Unit) {
			viewModel.load()

			viewModel.favoritesResultsFlow.collect { results ->
				favoritesFragmentDelegate.showResults(results)
			}
		}

		Column {
			MainToolbar(MainToolbarActiveButton.Favorites)

			var rowsSupportFragment by remember { mutableStateOf<RowsSupportFragment?>(null) }

			AndroidFragment<RowsSupportFragment>(
				modifier = Modifier
					.focusGroup()
					// The leanback code has its own awful focus handling that doesn't work properly with Compose view interop. To
					// workaround this issue we add custom behavior that only allows focus exit when the current selected row is the
					// first one. Additionally when we do switch the focus, we reset the leanback state so it won't cause weird
					// behavior when focus is regained
					.focusProperties {
						onExit = {
							val isFirstRowSelected = rowsSupportFragment?.selectedPosition?.let { it <= 0 } ?: false
							if (requestedFocusDirection != FocusDirection.Up || !isFirstRowSelected) {
								cancelFocusChange()
							} else {
								rowsSupportFragment?.selectedPosition = 0
								rowsSupportFragment?.verticalGridView?.clearFocus()
							}
						}
					}
					.padding(top = 5.dp)
					.fillMaxSize(),
				onUpdate = { fragment ->
					rowsSupportFragment = fragment
					fragment.adapter = favoritesFragmentDelegate.rowsAdapter
					fragment.onItemViewClickedListener = favoritesFragmentDelegate.onItemViewClickedListener
					fragment.onItemViewSelectedListener = favoritesFragmentDelegate.onItemViewSelectedListener
				}
			)
		}
	}
}
