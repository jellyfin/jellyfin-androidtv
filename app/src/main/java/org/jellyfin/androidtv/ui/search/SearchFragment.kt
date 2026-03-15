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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.leanback.app.RowsSupportFragment
import kotlinx.coroutines.channels.Channel
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.search.composable.SearchTextInput
import org.jellyfin.androidtv.ui.search.composable.SearchVoiceInput
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.androidtv.util.speech.rememberSpeechRecognizerAvailability
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
private enum class PendingFocusTarget {
	SearchInput,
	Results,
}

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
			val voiceInputFocusRequester = remember { FocusRequester() }
			val resultFocusRequester = remember { FocusRequester() }
			var hasResults by remember { mutableStateOf(false) }
			var resultsHasFocus by remember { mutableStateOf(false) }
			var autoShowIme by rememberSaveable { mutableStateOf(false) }
			var rowsSupportFragment by remember { mutableStateOf<RowsSupportFragment?>(null) }
			var searchEditText by remember { mutableStateOf<AppCompatEditText?>(null) }
			val focusRequestChannel = remember { Channel<PendingFocusTarget>(Channel.CONFLATED) }
			var suppressImeOnSearchFocusSignal by remember { mutableStateOf(0) }
			var suppressImeForNextSearchFocus by remember { mutableStateOf(false) }
			val speechRecognizerAvailability = rememberSpeechRecognizerAvailability()

			fun triggerSearchInputFocus(suppressIme: Boolean) {
				if (suppressIme) {
					suppressImeOnSearchFocusSignal += 1
					suppressImeForNextSearchFocus = true
				}
				focusRequestChannel.trySend(PendingFocusTarget.SearchInput)
			}

			fun requestResultsViewFocus() {
				val fragment = rowsSupportFragment ?: return
				if (fragment.selectedPosition < 0) {
					fragment.selectedPosition = 0
				}
				fragment.verticalGridView?.post {
					fragment.verticalGridView?.requestFocus()
					fragment.verticalGridView?.requestFocusFromTouch()
				}
			}

			LaunchedEffect(Unit) {
				val extraQuery = arguments?.getString(EXTRA_QUERY)
				if (!extraQuery.isNullOrBlank()) {
					query = query.copy(text = extraQuery)
					viewModel.searchImmediately(extraQuery)
					focusRequestChannel.trySend(PendingFocusTarget.Results)
				} else {
					textInputFocusRequester.requestFocus()
					autoShowIme = true
				}

				viewModel.searchResultsFlow.collect { results ->
					hasResults = results.any { it.items.isNotEmpty() }
					searchFragmentDelegate.showResults(results)
					if (!hasResults && resultsHasFocus) {
						textInputFocusRequester.requestFocus()
					}
				}
			}

			LaunchedEffect(Unit) {
				for (target in focusRequestChannel) {
					// Let focus changes (e.g., EditText losing focus on back) settle before requesting focus again.
					withFrameNanos { }
					withFrameNanos { }

					val focusResult = when (target) {
						PendingFocusTarget.SearchInput -> textInputFocusRequester.requestFocus()
						PendingFocusTarget.Results -> resultFocusRequester.requestFocus()
					}

					val finalResult = if (focusResult) {
						true
					} else {
						withFrameNanos { }
						val retryResult = when (target) {
							PendingFocusTarget.SearchInput -> textInputFocusRequester.requestFocus()
							PendingFocusTarget.Results -> resultFocusRequester.requestFocus()
						}
						retryResult
					}

					if (finalResult && target == PendingFocusTarget.SearchInput) {
						val suppressIme = suppressImeForNextSearchFocus
						suppressImeForNextSearchFocus = false
						rowsSupportFragment?.verticalGridView?.clearFocus()
						searchEditText?.post {
							searchEditText?.requestFocus()
							if (!suppressIme) {
								searchEditText?.requestFocusFromTouch()
							}
						}
					} else if (finalResult && target == PendingFocusTarget.Results) {
						requestResultsViewFocus()
					}
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
						.focusProperties {
							onExit = {
								if (requestedFocusDirection == FocusDirection.Down && !hasResults) {
									cancelFocusChange()
								}
							}
						}
						.padding(horizontal = 48.dp)
				) {
					if (speechRecognizerAvailability) {
						SearchVoiceInput(
							onQueryChange = { query = query.copy(text = it) },
							onQuerySubmit = {
								viewModel.searchImmediately(query.text)
								resultFocusRequester.requestFocus()
							},
							modifier = Modifier
								.focusRequester(voiceInputFocusRequester)
								.focusProperties { right = textInputFocusRequester },
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
							focusRequestChannel.trySend(PendingFocusTarget.Results)
						},
						onKeyboardDismissed = {
							if (hasResults) {
								focusRequestChannel.trySend(PendingFocusTarget.Results)
							} else {
								triggerSearchInputFocus(suppressIme = true)
							}
						},
						focusRequester = textInputFocusRequester,
						autoShowIme = autoShowIme,
						canNavigateDown = hasResults,
						onNavigateDown = {
							if (hasResults) {
								focusRequestChannel.trySend(PendingFocusTarget.Results)
							}
						},
						suppressImeOnFocusSignal = suppressImeOnSearchFocusSignal,
						onEditTextRef = { searchEditText = it },
						modifier = Modifier
							.weight(1f)
							.then(
								if (speechRecognizerAvailability) {
									Modifier.focusProperties { left = voiceInputFocusRequester }
								} else {
									Modifier
								}
							),
					)
				}

				AndroidFragment<RowsSupportFragment>(
					modifier = Modifier
						.focusGroup()
						.focusRequester(resultFocusRequester)
						.onFocusChanged { resultsHasFocus = it.hasFocus }
						.focusProperties {
							onEnter = {
								if (!hasResults) cancelFocusChange()
							}
							onExit = {
								val isFirstRowSelected = rowsSupportFragment?.selectedPosition?.let { it <= 0 } ?: false
								when (requestedFocusDirection) {
									FocusDirection.Up -> {
										if (isFirstRowSelected) {
											rowsSupportFragment?.selectedPosition = 0
											triggerSearchInputFocus(suppressIme = true)
										} else {
											cancelFocusChange()
										}
									}
									FocusDirection.Left,
									FocusDirection.Right,
									FocusDirection.Down -> cancelFocusChange()
									else -> Unit
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
