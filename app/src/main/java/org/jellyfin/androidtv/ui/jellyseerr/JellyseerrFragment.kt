package org.jellyfin.androidtv.ui.jellyseerr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.JellyseerrMovieDetails
import org.jellyfin.androidtv.data.repository.JellyseerrRequest
import org.jellyfin.androidtv.data.repository.JellyseerrSearchItem
import org.jellyfin.androidtv.data.repository.JellyseerrCast
import org.jellyfin.androidtv.data.repository.JellyseerrPersonDetails
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
import android.widget.ImageView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import kotlinx.coroutines.delay


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
private fun JellyseerrScreen(
	viewModel: JellyseerrViewModel = koinViewModel(),
) {
	val userPreferences = koinInject<UserPreferences>()
	val url = userPreferences[UserPreferences.jellyseerrUrl]
	val apiKey = userPreferences[UserPreferences.jellyseerrApiKey]
	val state by viewModel.uiState.collectAsState()

	Box(modifier = Modifier.fillMaxSize()) {
		val toastMessage = state.requestStatusMessage
		val isError = toastMessage?.contains(stringResource(R.string.jellyseerr_request_error), ignoreCase = true) == true

		LaunchedEffect(toastMessage) {
			if (toastMessage != null) {
				delay(3000)
				viewModel.clearRequestStatus()
			}
		}

		val toastAlpha by animateFloatAsState(
			targetValue = if (toastMessage != null) 1f else 0f,
			animationSpec = tween(durationMillis = 300),
		)
		val selectedItem = state.selectedItem
		if (selectedItem != null) {
			val backdropUrl = state.selectedMovie?.backdropPath ?: selectedItem.backdropPath

			if (!backdropUrl.isNullOrBlank()) {
				AsyncImage(
					modifier = Modifier
						.fillMaxSize()
						.graphicsLayer(alpha = 0.4f),
					url = backdropUrl,
					aspectRatio = 16f / 9f,
				)
			}
		}

		Column(modifier = Modifier.fillMaxSize()) {
			val state by viewModel.uiState.collectAsState()

			val showToolbar = !state.showAllTrendsGrid && state.selectedPerson == null

			if (showToolbar) {
				MainToolbar(MainToolbarActiveButton.Requests)
			}

			if (url.isBlank() || apiKey.isBlank()) {
				Text(
					text = stringResource(R.string.pref_jellyseerr_url_missing),
					modifier = Modifier.padding(24.dp),
				)
			} else {
				JellyseerrContent(viewModel = viewModel)
			}
		}

		if (toastAlpha > 0f && toastMessage != null) {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center,
			) {
				Box(
					modifier = Modifier
						.graphicsLayer(alpha = toastAlpha)
						.padding(horizontal = 24.dp)
						.background(
							color = if (isError) Color(0xCCB00020) else Color(0xCC00A060),
							shape = RoundedCornerShape(12.dp),
						)
						.border(2.dp, Color.White, RoundedCornerShape(12.dp))
						.padding(horizontal = 16.dp, vertical = 12.dp),
				) {
					Text(text = toastMessage, color = Color.White)
				}
			}
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
	val allTrendsListState = rememberLazyListState()
	val sectionSpacing = 16.dp
	val sectionInnerSpacing = 12.dp //Spacer Überschriften zu Inhalten

	BackHandler(enabled = state.selectedItem != null || state.showAllTrendsGrid || state.selectedPerson != null) {
		when {
			// Wenn ein Detail geöffnet ist, zuerst Detail schließen
			state.selectedItem != null -> viewModel.closeDetails()
			state.selectedPerson != null -> viewModel.closePerson()
			state.showAllTrendsGrid -> viewModel.closeAllTrends()
		}
	}


	val selectedItem = state.selectedItem
	val selectedPerson = state.selectedPerson

	if (selectedItem != null) {
		JellyseerrDetail(
			item = selectedItem,
			details = state.selectedMovie,
			requestStatusMessage = state.requestStatusMessage,
			onRequestClick = { seasons -> viewModel.request(selectedItem, seasons) },
			onCastClick = { castMember -> viewModel.showPerson(castMember) },
		)
	} else if (selectedPerson != null) {
		JellyseerrPersonScreen(
			person = selectedPerson,
			credits = state.personCredits,
			onCreditClick = { viewModel.showDetailsForItemFromPerson(it) },
		)
  	} else {
		val scrollState = rememberScrollState()
		val columnModifier = if (state.showAllTrendsGrid) {
			Modifier
				.fillMaxSize()
				.padding(24.dp)
		} else {
			Modifier
				.fillMaxSize()
				.verticalScroll(scrollState)
				.padding(24.dp)
		}

		Column(
			modifier = columnModifier,
		) {
			if (!state.showAllTrendsGrid) {
				Row(
					horizontalArrangement = Arrangement.spacedBy(12.dp),
				) {
					Box(
						modifier = Modifier
							.weight(1f),
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
							showKeyboardOnFocus = true,
						)
					}
				}

				Spacer(modifier = Modifier.size(sectionSpacing))
			}

			if (state.errorMessage != null) {
				Text(
					text = stringResource(R.string.jellyseerr_error_prefix, state.errorMessage ?: ""),
					color = Color.Red,
					modifier = Modifier.padding(bottom = 16.dp),
				)
			}

			if (state.showAllTrendsGrid) {
				Text(
					text = stringResource(R.string.jellyseerr_discover_title),
					color = Color.White,
				)

				val baseResults = if (state.query.isBlank()) {
					state.results.take(20)
				} else {
					state.results
				}

				if (baseResults.isEmpty() && !state.isLoading) {
					Text(
						text = stringResource(R.string.jellyseerr_no_results),
						modifier = Modifier.padding(vertical = 8.dp),
					)
				} else {
					val rows = state.results.chunked(5)

					LazyColumn(
						state = allTrendsListState,
						modifier = Modifier
							.fillMaxSize()
							.padding(top = 8.dp),
					) {
						items(rows.size) { rowIndex ->
							val rowItems = rows[rowIndex]

							Row(
								horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
								modifier = Modifier
									.fillMaxWidth()
									.padding(vertical = 15.dp), // Abstand zwischen den Reihen
							) {
								for (item in rowItems) {
									JellyseerrSearchCard(
										item = item,
										onClick = { viewModel.showDetailsForItem(item) },
									)
								}
							}

							if (rowIndex == rows.lastIndex && state.discoverHasMore && !state.isLoading) {
								LaunchedEffect(key1 = rows.size) {
									viewModel.loadMoreTrends()
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

				val baseResults = if (state.query.isBlank()) {
					state.results.take(20)
				} else {
					state.results
				}

				if (baseResults.isEmpty() && !state.isLoading) {
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
							.height(300.dp)
							.padding(top = 15.dp), // Abstand Label zu Karten
					) {
						val maxIndex = baseResults.lastIndex

						// Zeige "Alle Trends" nur, wenn keine Suche aktiv ist
						val extraItems = 1

						items(maxIndex + 1 + extraItems) { index ->
							when {
								index in 0..maxIndex -> {
									val item = baseResults[index]
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
						LaunchedEffect(baseResults) {
							if (baseResults.isNotEmpty()) {
								focusRequester.requestFocus()
							}
						}
					}
				}

				// Beliebte Filme
				if (state.selectedItem == null && state.selectedPerson == null && state.query.isBlank()) {
					Spacer(modifier = Modifier.size(sectionSpacing))

					Text(
						text = stringResource(R.string.jellyseerr_popular_title),
						color = JellyfinTheme.colorScheme.onBackground,
					)

					if (state.popularResults.isEmpty()) {
						Spacer(modifier = Modifier.size(sectionInnerSpacing))
						Text(
							text = stringResource(R.string.jellyseerr_no_results),
							modifier = Modifier.padding(horizontal = 24.dp),
							color = JellyfinTheme.colorScheme.onBackground,
						)
					} else {
						Spacer(modifier = Modifier.size(sectionInnerSpacing))

						LazyRow(
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							contentPadding = PaddingValues(horizontal = 24.dp),
							modifier = Modifier
								.fillMaxWidth()
								.height(300.dp),
						) {
							val maxIndex = state.popularResults.lastIndex
							val extraItems = 1

							items(maxIndex + 1 + extraItems) { index ->
								when {
									index in 0..maxIndex -> {
										val item = state.popularResults[index]
										JellyseerrSearchCard(
											item = item,
											onClick = { viewModel.showDetailsForItem(item) },
										)
									}
									index == maxIndex + 1 -> {
										JellyseerrViewAllCard(
											onClick = { viewModel.showAllPopularMovies() },
										)
									}
								}
							}
						}
					}
				}

				// Beliebte Serien
				if (state.selectedItem == null && state.selectedPerson == null && state.query.isBlank()) {
					Spacer(modifier = Modifier.size(sectionSpacing))

					Text(
						text = stringResource(R.string.jellyseerr_popular_tv_title),
						color = JellyfinTheme.colorScheme.onBackground,
					)

					if (state.popularTvResults.isEmpty()) {
						Spacer(modifier = Modifier.size(sectionInnerSpacing))
						Text(
							text = stringResource(R.string.jellyseerr_no_results),
							modifier = Modifier.padding(horizontal = 24.dp),
							color = JellyfinTheme.colorScheme.onBackground,
						)
					} else {
						Spacer(modifier = Modifier.size(sectionInnerSpacing))

						LazyRow(
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							contentPadding = PaddingValues(horizontal = 24.dp),
							modifier = Modifier
								.fillMaxWidth()
								.height(300.dp),
						) {
							val maxIndex = state.popularTvResults.lastIndex
							val extraItems = 1

							items(maxIndex + 1 + extraItems) { index ->
								when {
									index in 0..maxIndex -> {
										val item = state.popularTvResults[index]
										JellyseerrSearchCard(
											item = item,
											onClick = { viewModel.showDetailsForItem(item) },
										)
									}
									index == maxIndex + 1 -> {
										JellyseerrViewAllCard(
											onClick = { viewModel.showAllPopularTv() },
										)
									}
								}
							}
						}
					}
				}


				// Demnächst erscheinende Filme
				if (state.selectedItem == null && state.selectedPerson == null && state.query.isBlank()) {
					Spacer(modifier = Modifier.size(sectionSpacing))

					Text(
						text = stringResource(R.string.jellyseerr_upcoming_movies_title),
						color = JellyfinTheme.colorScheme.onBackground,
					)

					if (state.upcomingMovieResults.isEmpty()) {
						Spacer(modifier = Modifier.size(sectionInnerSpacing))
						Text(
							text = stringResource(R.string.jellyseerr_no_results),
							modifier = Modifier.padding(horizontal = 24.dp),
							color = JellyfinTheme.colorScheme.onBackground,
						)
					} else {
						Spacer(modifier = Modifier.size(sectionInnerSpacing))

						LazyRow(
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							contentPadding = PaddingValues(horizontal = 24.dp),
							modifier = Modifier
								.fillMaxWidth()
								.height(300.dp),
						) {
							val maxIndex = state.upcomingMovieResults.lastIndex
							val extraItems = 1

							items(maxIndex + 1 + extraItems) { index ->
								when {
									index in 0..maxIndex -> {
										val item = state.upcomingMovieResults[index]
										JellyseerrSearchCard(
											item = item,
											onClick = { viewModel.showDetailsForItem(item) },
										)
									}
									index == maxIndex + 1 -> {
										JellyseerrViewAllCard(
											onClick = { viewModel.showAllUpcomingMovies() },
										)
									}
								}
							}
						}
					}
				}

				// Demnächst erscheinende Serien
				if (state.selectedItem == null && state.selectedPerson == null && state.query.isBlank()) {
					Spacer(modifier = Modifier.size(sectionSpacing))

					Text(
						text = stringResource(R.string.jellyseerr_upcoming_tv_title),
						color = JellyfinTheme.colorScheme.onBackground,
					)

					if (state.upcomingTvResults.isEmpty()) {
						Spacer(modifier = Modifier.size(sectionInnerSpacing))
						Text(
							text = stringResource(R.string.jellyseerr_no_results),
							modifier = Modifier.padding(horizontal = 24.dp),
							color = JellyfinTheme.colorScheme.onBackground,
						)
					} else {
						Spacer(modifier = Modifier.size(sectionInnerSpacing))

						LazyRow(
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							contentPadding = PaddingValues(horizontal = 24.dp),
							modifier = Modifier
								.fillMaxWidth()
								.height(300.dp),
						) {
							val maxIndex = state.upcomingTvResults.lastIndex
							val extraItems = 1

							items(maxIndex + 1 + extraItems) { index ->
								when {
									index in 0..maxIndex -> {
										val item = state.upcomingTvResults[index]
										JellyseerrSearchCard(
											item = item,
											onClick = { viewModel.showDetailsForItem(item) },
										)
									}
									index == maxIndex + 1 -> {
										JellyseerrViewAllCard(
											onClick = { viewModel.showAllUpcomingTv() },
										)
									}
								}
							}
						}
					}
				}

				// Bisherige Anfragen (globale Liste)
				if (state.selectedItem == null && state.selectedPerson == null && state.query.isBlank()) {
					Spacer(modifier = Modifier.size(sectionSpacing))

					Text(
						text = stringResource(R.string.jellyseerr_recent_requests_title),
						color = JellyfinTheme.colorScheme.onBackground,
					)

					if (state.recentRequests.isEmpty()) {
						Spacer(modifier = Modifier.size(sectionInnerSpacing))
						Text(
							text = stringResource(R.string.jellyseerr_no_results),
							modifier = Modifier.padding(horizontal = 24.dp),
							color = JellyfinTheme.colorScheme.onBackground,
						)
					} else {
						Spacer(modifier = Modifier.size(sectionInnerSpacing))

						LazyRow(
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							contentPadding = PaddingValues(horizontal = 24.dp),
							modifier = Modifier
								.fillMaxWidth()
								.height(220.dp),
						) {
							items(state.recentRequests) { request ->
								JellyseerrRequestRow(
									request = request,
									onClick = { viewModel.showDetailsForRequest(request) },
								)
							}
						}
					}
				}
			}
		}
	}
}

@Composable
private fun JellyseerrPersonScreen(
    person: JellyseerrPersonDetails,
    credits: List<JellyseerrSearchItem>,
    onCreditClick: (JellyseerrSearchItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape),
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    url = person.profilePath,
                    aspectRatio = 1f,
                    scaleType = ImageView.ScaleType.CENTER_CROP,
                )
            }

            Column {
                Text(person.name, color = JellyfinTheme.colorScheme.onBackground)
                person.knownForDepartment.takeIf { !it.isNullOrBlank() }?.let {
                    Text(it, color = JellyfinTheme.colorScheme.onBackground)
                }
                person.placeOfBirth.takeIf { !it.isNullOrBlank() }?.let {
                    Text(it, color = JellyfinTheme.colorScheme.onBackground)
                }
            }
        }

        // Zusätzlicher Abstand, damit Karten nicht abgeschnitten werden
        Spacer(modifier = Modifier.size(32.dp))

        LazyColumn {
            val rows = credits.chunked(5)
            items(rows.size) { rowIndex ->
                val rowItems = rows[rowIndex]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp), // Abstand zwischen den Reihen
                ) {
                    rowItems.forEach { item ->
                        JellyseerrSearchCard(
                            item = item,
                            onClick = { onCreditClick(item) },
                        )
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
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(200.dp) // gleiche Höhe wie Poster
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                focusedContainerColor = Color.White,
                focusedContentColor = Color.Black,
            ),
        ) {
            Text(text = ">")
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
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(200.dp)
				.clip(RoundedCornerShape(12.dp))
				.border(
					width = if (isFocused) 2.dp else 1.dp,
					color = Color.White,
					shape = RoundedCornerShape(12.dp),
				),
		) {
			AsyncImage(
				modifier = Modifier.fillMaxSize(),
				url = item.posterPath,
				aspectRatio = 2f / 3f,
				scaleType = ImageView.ScaleType.CENTER_CROP,
			)

			when {
				item.isAvailable -> {
					Box(
						modifier = Modifier
							.align(Alignment.TopEnd)
							.padding(6.dp)
							.clip(RoundedCornerShape(999.dp))
							.background(Color(0xFF00C800)),
					) {
						androidx.compose.foundation.Image(
							imageVector = ImageVector.vectorResource(id = R.drawable.ic_check),
							contentDescription = null,
							modifier = Modifier
								.padding(4.dp)
								.size(16.dp),
						)
					}
				}
				item.isRequested -> {
					Box(
						modifier = Modifier
							.align(Alignment.TopEnd)
							.padding(6.dp)
							.clip(RoundedCornerShape(999.dp))
							.background(Color(0xFFAA5CC3)),
					) {
						androidx.compose.foundation.Image(
							imageVector = ImageVector.vectorResource(id = R.drawable.ic_time),
							contentDescription = null,
							modifier = Modifier
								.padding(4.dp)
								.size(16.dp),
						)
					}
				}
			}
		}

		Spacer(modifier = Modifier.size(4.dp))

		Text(
			text = item.title,
			color = JellyfinTheme.colorScheme.onBackground,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.padding(horizontal = 4.dp),
		)

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
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(160.dp)
				.clip(RoundedCornerShape(12.dp))
				.border(
					width = if (isFocused) 2.dp else 1.dp,
					color = Color.White,
					shape = RoundedCornerShape(12.dp),
				),
		) {
			AsyncImage(
				modifier = Modifier.fillMaxSize(),
				url = request.posterPath,
				aspectRatio = 2f / 3f,
				scaleType = ImageView.ScaleType.CENTER_CROP,
			)

			// Verfügbar / angefragt Badges anhand request.status
			when {
				request.status == 5 -> {
					Box(
						modifier = Modifier
							.align(Alignment.TopEnd)
							.padding(6.dp)
							.clip(RoundedCornerShape(999.dp))
							.background(Color(0xFF00C800)),
					) {
						androidx.compose.foundation.Image(
							imageVector = ImageVector.vectorResource(id = R.drawable.ic_check),
							contentDescription = null,
							modifier = Modifier
								.padding(4.dp)
								.size(16.dp),
						)
					}
				}
				request.status != null -> {
					Box(
						modifier = Modifier
							.align(Alignment.TopEnd)
							.padding(6.dp)
							.clip(RoundedCornerShape(999.dp))
							.background(Color(0xFFAA5CC3)),
					) {
						androidx.compose.foundation.Image(
							imageVector = ImageVector.vectorResource(id = R.drawable.ic_time),
							contentDescription = null,
							modifier = Modifier
								.padding(4.dp)
								.size(16.dp),
						)
					}
				}
			}
		}

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
private fun JellyseerrCastRow(
	cast: List<JellyseerrCast>,
	onCastClick: (JellyseerrCast) -> Unit,
) {
	val displayCast = cast.take(10)

	LazyRow(
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 4.dp),
	) {
		items(displayCast) { person ->
			JellyseerrCastCard(
				person = person,
				onClick = { onCastClick(person) },
			)
		}
	}
}

@Composable
private fun JellyseerrCastCard(
	person: JellyseerrCast,
	onClick: () -> Unit,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val scale = if (isFocused) 1.05f else 1f

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.width(120.dp)
			.padding(vertical = 4.dp)
			.clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
			.focusable(interactionSource = interactionSource)
			.graphicsLayer(
				scaleX = scale,
				scaleY = scale,
			),
	) {
		Box(
			modifier = Modifier
				.size(80.dp)
				.clip(CircleShape)
				.border(
					width = if (isFocused) 2.dp else 1.dp,
					color = Color.White,
					shape = CircleShape,
				),
		) {
			AsyncImage(
				modifier = Modifier.fillMaxSize(),
				url = person.profilePath,
				aspectRatio = 1f,
				scaleType = ImageView.ScaleType.CENTER_CROP,
			)
		}

		Spacer(modifier = Modifier.size(4.dp))

		Text(
			text = person.name,
			color = JellyfinTheme.colorScheme.onBackground,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)

		person.character?.takeIf { it.isNotBlank() }?.let { role ->
			Text(
				text = role,
				color = JellyfinTheme.colorScheme.onBackground,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		}
	}
}

@Composable
private fun JellyseerrDetail(
	item: JellyseerrSearchItem,
	details: JellyseerrMovieDetails?,
	requestStatusMessage: String?,
	onRequestClick: (List<Int>?) -> Unit,
	onCastClick: (JellyseerrCast) -> Unit,
) {
	val requestButtonFocusRequester = remember { FocusRequester() }
	val requestButtonInteractionSource = remember { MutableInteractionSource() }
	val requestButtonFocused by requestButtonInteractionSource.collectIsFocusedAsState()
	val requestButtonScale = if (requestButtonFocused) 1.05f else 1f
	val navigationRepository = koinInject<org.jellyfin.androidtv.ui.navigation.NavigationRepository>()
	val isTv = item.mediaType == "tv"
	val availableSeasons = details?.seasons
		?.filter { it.seasonNumber > 0 }
		?.sortedBy { it.seasonNumber }
		.orEmpty()
	var selectedSeasons by remember(item.id) { mutableStateOf<Set<Int>>(emptySet()) }

		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(24.dp),
		) {
			Row(
				horizontalArrangement = Arrangement.spacedBy(24.dp),
			) {
				Column(
					verticalArrangement = Arrangement.spacedBy(12.dp),
				) {
					Box(
						modifier = Modifier
							.width(200.dp)
							.height(300.dp)
							.clip(RoundedCornerShape(16.dp))
							.border(
								width = 2.dp,
								color = Color.White,
								shape = RoundedCornerShape(16.dp),
							),
					) {
						AsyncImage(
							modifier = Modifier.fillMaxSize(),
							url = details?.posterPath ?: item.posterPath,
							aspectRatio = 2f / 3f,
							scaleType = ImageView.ScaleType.CENTER_CROP,
						)
					}

					if (isTv && availableSeasons.isNotEmpty()) {
						Spacer(modifier = Modifier.size(12.dp))

						Text(
							text = stringResource(R.string.jellyseerr_seasons_label),
							color = JellyfinTheme.colorScheme.onBackground,
						)

						Spacer(modifier = Modifier.size(8.dp))

						LazyRow(
							horizontalArrangement = Arrangement.spacedBy(8.dp),
							modifier = Modifier.fillMaxWidth(),
						) {
							item {
								val allSelected = selectedSeasons.size == availableSeasons.size && availableSeasons.isNotEmpty()
								Button(
									onClick = {
										selectedSeasons = if (allSelected) {
											emptySet()
										} else {
											availableSeasons.map { it.seasonNumber }.toSet()
										}
									},
									colors = if (allSelected) {
										ButtonDefaults.colors(
											containerColor = JellyfinTheme.colorScheme.buttonFocused,
											contentColor = JellyfinTheme.colorScheme.onButtonFocused,
										)
									} else {
										ButtonDefaults.colors(
											containerColor = Color.Transparent,
											contentColor = JellyfinTheme.colorScheme.onBackground,
										)
									},
								) {
									Text(text = stringResource(R.string.jellyseerr_seasons_all))
								}
							}

							items(availableSeasons) { season ->
								val number = season.seasonNumber
								val selected = selectedSeasons.contains(number)
								val label = season.name?.takeIf { it.isNotBlank() } ?: "S$number"

								Button(
									onClick = {
										selectedSeasons = if (selected) {
											selectedSeasons - number
										} else {
											selectedSeasons + number
										}
									},
									colors = if (selected) {
										ButtonDefaults.colors(
											containerColor = JellyfinTheme.colorScheme.buttonFocused,
											contentColor = JellyfinTheme.colorScheme.onButtonFocused,
										)
									} else {
										ButtonDefaults.colors(
											containerColor = Color.Transparent,
											contentColor = JellyfinTheme.colorScheme.onBackground,
										)
									},
								) {
									Text(text = label)
								}
							}
						}
					}

					val isRequested = item.isRequested
					val isAvailable = item.isAvailable

					val buttonColors = when {
						isAvailable -> ButtonDefaults.colors(
							containerColor = Color(0xFF00C800),
							contentColor = Color.White,
							focusedContainerColor = Color(0xFF00E000),
							focusedContentColor = Color.White,
						)
						isRequested -> ButtonDefaults.colors(
							containerColor = Color(0xFFFFA000),
							contentColor = Color.Black,
							focusedContainerColor = Color(0xFFFFC107),
							focusedContentColor = Color.Black,
						)
						else -> ButtonDefaults.colors(
							containerColor = Color(0xFFAA5CC3),
							contentColor = Color.White,
							focusedContainerColor = Color(0xFFBB86FC),
							focusedContentColor = Color.White,
						)
					}

					val buttonText = when {
						isAvailable -> stringResource(R.string.lbl_play)
						isRequested -> stringResource(R.string.jellyseerr_requested_label)
						else -> stringResource(R.string.jellyseerr_request_button)
					}

					Button(
						onClick = {
							when {
								isAvailable -> {
									navigationRepository.navigate(
										org.jellyfin.androidtv.ui.navigation.Destinations.search(item.title),
									)
								}
								else -> {
									val seasonsParam = if (isTv) {
										selectedSeasons.toList().sorted().takeIf { it.isNotEmpty() }
									} else {
										null
									}
									onRequestClick(seasonsParam)
								}
							}
						},
						enabled = !isRequested,
						colors = buttonColors,
						interactionSource = requestButtonInteractionSource,
						modifier = Modifier
							.focusRequester(requestButtonFocusRequester)
							.focusable(interactionSource = requestButtonInteractionSource)
							.graphicsLayer(
								scaleX = requestButtonScale,
								scaleY = requestButtonScale,
							),
					) {
						Text(text = buttonText)
					}

				}

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

					val cast = details?.credits?.cast.orEmpty()
					if (cast.isNotEmpty()) {
						Spacer(modifier = Modifier.size(16.dp))

						Text(
							text = stringResource(id = R.string.jellyseerr_cast_title),
							color = JellyfinTheme.colorScheme.onBackground,
						)

						Spacer(modifier = Modifier.size(8.dp))

						JellyseerrCastRow(
							cast = cast,
							onCastClick = onCastClick,
						)
					}

					Spacer(modifier = Modifier.size(16.dp))
				}
			}
		}

	LaunchedEffect(Unit) {
		kotlinx.coroutines.delay(150)
		requestButtonFocusRequester.requestFocus()
	}
}
