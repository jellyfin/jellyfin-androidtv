package org.jellyfin.androidtv.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.leanback.app.RowsSupportFragment
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.search.composable.SearchTextInput
import org.jellyfin.androidtv.ui.search.composable.SearchVoiceInput
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.androidtv.util.speech.rememberSpeechRecognizerAvailability
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class SearchFragment : Fragment() {
	companion object {
		const val EXTRA_QUERY = "query"
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		JellyfinTheme {
			val viewModel = koinViewModel<SearchViewModel>()
			val searchFragmentDelegate = koinInject<SearchFragmentDelegate> { parametersOf(requireContext()) }
			var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
			val textInputFocusRequester = remember { FocusRequester() }
			val resultFocusRequester = remember { FocusRequester() }
			val speechRecognizerAvailability = rememberSpeechRecognizerAvailability()

			LaunchedEffect(Unit) {
				val extraQuery = arguments?.getString(EXTRA_QUERY)
				if (!extraQuery.isNullOrBlank()) {
					query = query.copy(text = extraQuery)
					viewModel.searchImmediately(extraQuery)
					resultFocusRequester.requestFocus()
				} else {
					textInputFocusRequester.requestFocus()
				}

				viewModel.searchResultsFlow.collect { results ->
					searchFragmentDelegate.showResults(results)
				}
			}

			Column {
				MainToolbar(MainToolbarActiveButton.Search)

				Row(
					horizontalArrangement = Arrangement.spacedBy(12.dp),
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier
						.focusRestorer()
						.focusGroup()
						.padding(horizontal = 48.dp)
				) {
					if (speechRecognizerAvailability) {
						SearchVoiceInput(
							onQueryChange = { query = query.copy(text = it) },
							onQuerySubmit = {
								viewModel.searchImmediately(query.text)
								resultFocusRequester.requestFocus()
							}
						)
					}

					SearchTextInput(
						query = query.text,
						onQueryChange = {
							query = query.copy(text = it)
							viewModel.searchDebounced(query.text)
						},
						onQuerySubmit = {
							viewModel.searchImmediately(query.text)
							// Note: We MUST change the focus to somewhere else when the keyboard is submitted because some vendors (like Amazon)
							// will otherwise just keep showing a (fullscreen) keyboard, soft-locking the app.
							resultFocusRequester.requestFocus()
						},
						modifier = Modifier
							.weight(1f)
							.focusRequester(textInputFocusRequester),
					)
				}

				// The leanback code has its own awful focus handling that doesn't work properly with Compose view inteop to workaround this
				// issue we add custom behavior that only allows focus exit when the current selected row is the first one. Additionally when
				// we do switch the focus, we reset the leanback state so it won't cause weird behavior when focus is regained
				var rowsSupportFragment by remember { mutableStateOf<RowsSupportFragment?>(null) }

				AndroidFragment<RowsSupportFragment>(
					modifier = Modifier
						.focusGroup()
						.focusRequester(resultFocusRequester)
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
						fragment.adapter = searchFragmentDelegate.rowsAdapter
						fragment.onItemViewClickedListener = searchFragmentDelegate.onItemViewClickedListener
						fragment.onItemViewSelectedListener = searchFragmentDelegate.onItemViewSelectedListener
					}
				)
			}
		}
	}
}
