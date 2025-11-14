package org.jellyfin.androidtv.ui.jellyseerr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.focusable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.JellyseerrMovieDetails
import org.jellyfin.androidtv.data.repository.JellyseerrRequest
import org.jellyfin.androidtv.data.repository.JellyseerrSearchItem
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.search.composable.SearchTextInput
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class JellyseerrFragment : Fragment() {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	) = content {
		JellyfinTheme {
			JellyseerrScreen()
		}
	}
}

@Composable
private fun JellyseerrScreen() {
	val userPreferences = koinInject<UserPreferences>()
	val url = userPreferences[UserPreferences.jellyseerrUrl]
	val apiKey = userPreferences[UserPreferences.jellyseerrApiKey]

	Column(modifier = Modifier.fillMaxSize()) {
		MainToolbar(MainToolbarActiveButton.Requests)

		if (url.isBlank() || apiKey.isBlank()) {
			Text(
				text = stringResource(R.string.pref_jellyseerr_url_missing),
				modifier = Modifier
					.padding(32.dp),
			)
		} else {
			JellyseerrContent()
		}
	}
}

@Composable
private fun JellyseerrContent(
	viewModel: JellyseerrViewModel = koinViewModel(),
) {
	val state by viewModel.uiState.collectAsState()
	val keyboardController = LocalSoftwareKeyboardController.current
	val searchFocusRequester = remember { FocusRequester() }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
	) {
		if (state.selectedItem == null && !state.showAllTrendsGrid) {
			Row(
				horizontalArrangement = Arrangement.spacedBy(12.dp),
			) {
				Box(
					modifier = Modifier
						.weight(1f)
						.clickable {
							searchFocusRequester.requestFocus()
							keyboardController?.show()
						},
				) {
					SearchTextInput(
						query = state.query,
						onQueryChange = { viewModel.updateQuery(it) },
						onQuerySubmit = {
							viewModel.search()
							keyboardController?.hide()
						},
						modifier = Modifier
							.fillMaxWidth()
							.focusRequester(searchFocusRequester),
						showKeyboardOnFocus = false,
					)
				}
			}

			Spacer(modifier = Modifier.size(16.dp))
		}

		BackHandler(enabled = state.selectedItem != null || state.showAllTrendsGrid) {
			when {
				state.selectedItem != null -> viewModel.closeDetails()
				state.showAllTrendsGrid -> viewModel.closeAllTrends()
			}
		}

		if (state.errorMessage != null) {
			Text(
				text = stringResource(R.string.jellyseerr_error_prefix, state.errorMessage ?: ""),
				color = Color.Red,
				modifier = Modifier.padding(bottom = 16.dp),
			)
		}

		// Detailansicht
		val selectedItem = state.selectedItem
		if (selectedItem != null) {
			JellyseerrDetail(
				item = selectedItem,
				details = state.selectedMovie,
				onRequestClick = { viewModel.request(selectedItem) },
				// onClose brauchst du nicht mehr, wenn du BackHandler benutzt
			)
		} else if (state.showAllTrendsGrid) {
			// HIER kommt dein gepostetes Snippet hin:
			Text(text = stringResource(R.string.jellyseerr_discover_title))

			if (state.results.isEmpty() && !state.isLoading) {
				Text(
					text = stringResource(R.string.jellyseerr_no_results),
					modifier = Modifier.padding(vertical = 8.dp),
				)
			} else {
				LazyColumn(
					modifier = Modifier
						.fillMaxSize()
						.padding(top = 8.dp),
				) {
					items(state.results.chunked(5)) { rowItems ->
						Row(
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							modifier = Modifier.padding(vertical = 4.dp),
						) {
							for (item in rowItems) {
								JellyseerrSearchCard(
									item = item,
									onClick = { viewModel.showDetailsForItem(item) },
								)
							}
						}
					}
				}
			}
		} else {
			val titleRes = if (state.query.isBlank()) {
				R.string.jellyseerr_discover_title
			} else {
				R.string.jellyseerr_search_results_title
			}
			Text(
				text = stringResource(titleRes),
				color = JellyfinTheme.colorScheme.onBackground,
			)

			if (state.results.isEmpty() && !state.isLoading) {
				Text(
					text = stringResource(R.string.jellyseerr_no_results),
					modifier = Modifier.padding(vertical = 8.dp),
				)
			} else {
				// Fokus zuerst auf dem ersten Element der Discover-Slides setzen
				val focusRequester = FocusRequester()

				LazyRow(
					horizontalArrangement = Arrangement.spacedBy(12.dp),
					contentPadding = PaddingValues(horizontal = 24.dp),
					modifier = Modifier
						.fillMaxWidth()
						.height(260.dp)
						.padding(top = 8.dp),
				) {
					val maxIndex = state.results.lastIndex

					items(maxIndex + 2) { index ->
						when {
							index in 0..maxIndex -> {
								val item = state.results[index]
								val cardModifier = if (index == 0) {
									Modifier.focusRequester(focusRequester)
								} else {
									Modifier
								}

								JellyseerrSearchCard(
									item = item,
									onClick = { viewModel.showDetailsForItem(item) },
									modifier = cardModifier,
								)
							}
							index == maxIndex + 1 -> {
								JellyseerrViewAllCard(
									onClick = { viewModel.showAllTrends() },
								)
							}
						}
					}
				}

				if (state.query.isBlank() && state.selectedItem == null) {
					LaunchedEffect(state.results) {
						if (state.results.isNotEmpty()) {
							focusRequester.requestFocus()
						}
					}
				}
			}
		}
	}
}

@Composable
private fun JellyseerrViewAllCard(
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .fillMaxSize()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            Text(
                text = stringResource(R.string.jellyseerr_view_all_trends),
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.Center),
            )
        }
    }
}

@Composable
private fun JellyseerrSearchCard(
	item: JellyseerrSearchItem,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val scale = if (isFocused) 1.1f else 1f

	Column(
		modifier = modifier
			.width(150.dp)
			.fillMaxSize()
			.clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
			.focusable(interactionSource = interactionSource)
			.graphicsLayer(
				scaleX = scale,
				scaleY = scale,
			)
			.padding(vertical = 4.dp),
	) {
		AsyncImage(
			modifier = Modifier
				.fillMaxWidth()
				.height(200.dp),
			url = item.posterPath,
			aspectRatio = 2f / 3f,
		)

		Spacer(modifier = Modifier.size(4.dp))

		Text(
			text = item.title,
			color = JellyfinTheme.colorScheme.onBackground,
			maxLines = 2,
			overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
			modifier = Modifier.padding(horizontal = 4.dp),
		)

		if (item.isRequested) {
			Text(
				text = stringResource(R.string.jellyseerr_requested_label),
				color = JellyfinTheme.colorScheme.onBackground,
				modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
			)
		}
	}
}

@Composable
private fun JellyseerrRequestRow(
	request: JellyseerrRequest,
	onClick: () -> Unit,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val scale = if (isFocused) 1.1f else 1f

	Column(
		modifier = Modifier
			.width(150.dp)
			.fillMaxHeight()
			.padding(vertical = 4.dp)
			.clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
			.focusable(interactionSource = interactionSource)
			.graphicsLayer(
				scaleX = scale,
				scaleY = scale,
			),
	) {
		AsyncImage(
			modifier = Modifier
				.fillMaxWidth()
				.height(160.dp)
				.graphicsLayer(
					scaleX = scale,
					scaleY = scale,
				),
			url = request.posterPath,
			aspectRatio = 2f / 3f,
		)

		Spacer(modifier = Modifier.size(4.dp))

		Text(
			text = request.title,
			color = JellyfinTheme.colorScheme.onBackground,
			modifier = Modifier.padding(horizontal = 4.dp),
		)
		request.mediaType?.let {
			val typeText = when (it) {
				"movie" -> stringResource(R.string.lbl_movies)
				"tv" -> stringResource(R.string.lbl_tv_series)
				else -> it
			}
			Text(
				text = typeText,
				color = JellyfinTheme.colorScheme.onBackground,
			)
		}
	}
}

@Composable
private fun JellyseerrDetail(
	item: JellyseerrSearchItem,
	details: JellyseerrMovieDetails?,
	onRequestClick: () -> Unit
) {
	val requestButtonFocusRequester = remember { FocusRequester() }

Box(
    modifier = Modifier
        .fillMaxSize()
) {
    // Hintergrund
    AsyncImage(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.4f), // leicht abdunkeln, falls nötig
        url = details?.backdropPath ?: item.backdropPath,
        aspectRatio = 16f / 9f,
    )
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(top = 16.dp),
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(16.dp),
		) {
			AsyncImage(
				modifier = Modifier
					.width(200.dp)
					.height(300.dp),
				url = details?.posterPath ?: item.posterPath,
				aspectRatio = 2f / 3f,
			)

			Column(
				modifier = Modifier.weight(1f),
			) {
				Text(text = details?.title ?: item.title, color = JellyfinTheme.colorScheme.onBackground)

				Spacer(modifier = Modifier.size(4.dp))

				val year = details?.releaseDate?.take(4)
				val runtime = details?.runtime
				val rating = details?.voteAverage

				val metaParts = buildList {
					year?.let { add(it) }
					runtime?.let { add("${it} min") }
					rating?.let { add(String.format("%.1f/10", it)) }
				}

				if (metaParts.isNotEmpty()) {
					Text(
						text = metaParts.joinToString(" • "),
						color = JellyfinTheme.colorScheme.onBackground,
					)
				}

				val genres = details?.genres?.joinToString(", ") { it.name }.orEmpty()
				if (genres.isNotBlank()) {
					Spacer(modifier = Modifier.size(4.dp))
					Text(
						text = genres,
						color = JellyfinTheme.colorScheme.onBackground,
					)
				}

				Spacer(modifier = Modifier.size(8.dp))

				if (!details?.overview.isNullOrBlank()) {
					Text(text = details.overview!!, color = JellyfinTheme.colorScheme.onBackground)
				} else if (!item.overview.isNullOrBlank()) {
					Text(text = item.overview!!, color = JellyfinTheme.colorScheme.onBackground)
				}

				Spacer(modifier = Modifier.size(16.dp))

				Row(
					horizontalArrangement = Arrangement.spacedBy(12.dp),
				) {
					Button(
						onClick = onRequestClick,
						colors = ButtonDefaults.colors(),
						modifier = Modifier
							.focusRequester(requestButtonFocusRequester)
							.focusable(),
					) {
						Text(text = stringResource(R.string.jellyseerr_request_button))
					}

					LaunchedEffect(item.id, details?.id) {
						requestButtonFocusRequester.requestFocus()
					}
				}
			}
		}
	}
}
	LaunchedEffect(item.id, details?.id) {
		requestButtonFocusRequester.requestFocus()
	}
}
