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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.JellyseerrMovieDetails
import org.jellyfin.androidtv.data.repository.JellyseerrEpisode
import org.jellyfin.androidtv.data.repository.JellyseerrRequest
import org.jellyfin.androidtv.data.repository.JellyseerrSearchItem
import org.jellyfin.androidtv.data.repository.JellyseerrCast
import org.jellyfin.androidtv.data.repository.JellyseerrPersonDetails
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Icon
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

private const val VIEW_ALL_TRENDING = "view_all_trending"
private const val VIEW_ALL_POPULAR_MOVIES = "view_all_popular_movies"
private const val VIEW_ALL_POPULAR_TV = "view_all_popular_tv"
private const val VIEW_ALL_UPCOMING_MOVIES = "view_all_upcoming_movies"
private const val VIEW_ALL_UPCOMING_TV = "view_all_upcoming_tv"


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
	var showSeasonDialog by remember { mutableStateOf(false) }
	val firstCastFocusRequester = remember { FocusRequester() }

	// Backdrop Rotation State
	var currentBackdropUrl by remember { mutableStateOf<String?>(null) }

	// Rotiere Backdrop alle 10 Sekunden
	LaunchedEffect(state.results) {
		while (true) {
			val itemsWithBackdrop = state.results
				.take(20)
				.mapNotNull { it.backdropPath }
				.filter { it.isNotBlank() }

			if (itemsWithBackdrop.isNotEmpty()) {
				currentBackdropUrl = itemsWithBackdrop.random()
			}

			delay(10000)
		}
	}

	// Dialog schließen wenn selectedItem sich ändert (z.B. beim Zurücknavigieren)
	LaunchedEffect(state.selectedItem) {
		if (state.selectedItem == null) {
			showSeasonDialog = false
		}
	}

	// Fokussiere ersten Cast-Eintrag wenn Dialog geschlossen wird
	LaunchedEffect(showSeasonDialog) {
		if (!showSeasonDialog && state.selectedItem != null) {
			kotlinx.coroutines.delay(100)
			try {
				firstCastFocusRequester.requestFocus()
			} catch (e: IllegalStateException) {
				// Ignore if no focusable found
			}
		}
	}

	// BackHandler für Dialog
	BackHandler(enabled = showSeasonDialog) {
		showSeasonDialog = false
	}

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
		} else if (state.selectedPerson == null) {
			// Crossfade für weichen Übergang zwischen Backdrops
			androidx.compose.animation.Crossfade(
				targetState = currentBackdropUrl,
				animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000),
				label = "backdrop_crossfade"
			) { backdropUrl ->
				if (backdropUrl != null) {
					AsyncImage(
						modifier = Modifier
							.fillMaxSize()
							.graphicsLayer(alpha = 0.4f),
						url = backdropUrl,
						aspectRatio = 16f / 9f,
						scaleType = android.widget.ImageView.ScaleType.CENTER_CROP,
					)
				}
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
				JellyseerrContent(
					viewModel = viewModel,
					onShowSeasonDialog = { showSeasonDialog = true },
					firstCastFocusRequester = firstCastFocusRequester,
				)
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
		
		// Season Dialog auf oberster Ebene (über allem anderen)
		if (showSeasonDialog && state.selectedItem != null) {
			val selectedItem = state.selectedItem!!
			val details = state.selectedMovie
			val navigationRepository = koinInject<org.jellyfin.androidtv.ui.navigation.NavigationRepository>()
			val availableSeasons = details?.seasons
				?.filter { it.seasonNumber > 0 }
				?.sortedBy { it.seasonNumber }
				.orEmpty()

			Dialog(
				onDismissRequest = {
					showSeasonDialog = false
					// Refresh details to sync with Jellyseerr server when dialog closes
					viewModel.refreshCurrentDetails()
				},
				properties = DialogProperties(usePlatformDefaultWidth = false),
			) {
				val firstButtonFocusRequester = remember { FocusRequester() }
				val dialogBackdropUrl = details?.backdropPath ?: selectedItem.backdropPath
				val overlayBrush = Brush.verticalGradient(
					colors = listOf(
						JellyfinTheme.colorScheme.popover.copy(alpha = 0.85f),
						JellyfinTheme.colorScheme.popover.copy(alpha = 0.95f),
					),
				)

				Box(
					modifier = Modifier
						.fillMaxWidth()
						.fillMaxHeight(),
				) {
					if (!dialogBackdropUrl.isNullOrBlank()) {
						AsyncImage(
							modifier = Modifier.matchParentSize(),
							url = dialogBackdropUrl,
							aspectRatio = 16f / 9f,
							scaleType = ImageView.ScaleType.CENTER_CROP,
						)
					} else {
						Box(
							modifier = Modifier
								.matchParentSize()
								.background(JellyfinTheme.colorScheme.popover),
						)
					}

					Box(
						modifier = Modifier
							.matchParentSize()
							.background(overlayBrush),
					)

					Column(
						modifier = Modifier
							.fillMaxSize()
							.padding(16.dp),
						verticalArrangement = Arrangement.spacedBy(12.dp),
					) {
						Text(
							text = stringResource(R.string.jellyseerr_seasons_label),
							color = JellyfinTheme.colorScheme.onBackground,
						)

						Spacer(modifier = Modifier.size(8.dp))

						val expandedSeasons = remember { mutableStateMapOf<Int, Boolean>() }

						LazyColumn(
							modifier = Modifier.weight(1f),
						) {
							itemsIndexed(
								items = availableSeasons,
								key = { _, season -> season.seasonNumber },
							) { index, season ->
								val number = season.seasonNumber
								val episodeCount = season.episodeCount

								// Jellyseerr Status Codes:
								// null = Nicht angefragt
								// 1 = Pending approval, 2 = Approved, 3 = Declined, 4 = Processing, 5 = Available
								val jellyseerrStatus = season.status

								// Staffel ist verfügbar wenn status = 5 (Available in Jellyseerr)
								val isAvailable = jellyseerrStatus == 5

								// Staffel ist angefragt wenn status vorhanden ist (1-4) aber nicht verfügbar
								val seasonRequested = jellyseerrStatus != null && jellyseerrStatus != 5

								val expanded = expandedSeasons[number] == true
								val seasonKey = SeasonKey(selectedItem.id, number)

								val buttonInteraction = remember { MutableInteractionSource() }
								val buttonFocused by buttonInteraction.collectIsFocusedAsState()

								val buttonColors = when {
									isAvailable -> ButtonDefaults.colors(
										containerColor = Color(0xFF00A800),
										contentColor = Color.White,
										focusedContainerColor = Color(0xFF00FF00),
										focusedContentColor = Color.Black,
									)
									seasonRequested -> ButtonDefaults.colors(
										containerColor = Color(0xFFDD8800),
										contentColor = Color.Black,
										focusedContainerColor = Color(0xFFFFBB00),
										focusedContentColor = Color.Black,
									)
									else -> ButtonDefaults.colors(
										containerColor = Color(0xFF9933CC),
										contentColor = Color.White,
										focusedContainerColor = Color(0xFFDD66FF),
										focusedContentColor = Color.Black,
									)
								}

								val buttonText = when {
									isAvailable -> stringResource(R.string.lbl_play)
									seasonRequested -> stringResource(R.string.jellyseerr_requested_label)
									else -> stringResource(R.string.jellyseerr_request_button)
								}

								val buttonModifier = if (index == 0) {
									Modifier.focusRequester(firstButtonFocusRequester)
								} else {
									Modifier
								}

								// Season Card mit vollem Layout
								Column(
									modifier = Modifier
										.fillMaxWidth()
										.padding(vertical = 8.dp)
										.background(
											Color.Black.copy(alpha = 0.4f),
											RoundedCornerShape(12.dp)
										)
										.padding(12.dp),
								) {
									Row(
										modifier = Modifier.fillMaxWidth(),
										horizontalArrangement = Arrangement.spacedBy(16.dp),
									) {
										// Season Poster
										Box(
											modifier = Modifier
												.width(100.dp)
												.height(150.dp)
												.clip(RoundedCornerShape(8.dp))
												.background(Color.Gray.copy(alpha = 0.3f)),
										) {
											if (!season.posterPath.isNullOrBlank()) {
												AsyncImage(
													modifier = Modifier.fillMaxSize(),
													url = season.posterPath,
													aspectRatio = 2f / 3f,
													scaleType = ImageView.ScaleType.CENTER_CROP,
												)
											} else {
												Box(
													modifier = Modifier.fillMaxSize(),
													contentAlignment = Alignment.Center,
												) {
													androidx.compose.foundation.Image(
														imageVector = ImageVector.vectorResource(id = R.drawable.ic_clapperboard),
														contentDescription = null,
														modifier = Modifier.size(40.dp),
														colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF888888)),
													)
												}
											}

											// Status Badge
											when {
												isAvailable -> {
													Box(
														modifier = Modifier
															.align(Alignment.TopEnd)
															.padding(4.dp)
															.clip(RoundedCornerShape(999.dp))
															.background(Color(0xFF00A800)),
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
												seasonRequested -> {
													Box(
														modifier = Modifier
															.align(Alignment.TopEnd)
															.padding(4.dp)
															.clip(RoundedCornerShape(999.dp))
															.background(Color(0xFFDD8800)),
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

										// Season Details
										Column(
											modifier = Modifier.weight(1f),
											verticalArrangement = Arrangement.spacedBy(6.dp),
										) {
											// Season Title
											Text(
												text = season.name ?: "Staffel $number",
												color = JellyfinTheme.colorScheme.onBackground,
												fontSize = 18.sp,
												maxLines = 1,
												overflow = TextOverflow.Ellipsis,
											)

											// Meta Info Row
											Row(
												horizontalArrangement = Arrangement.spacedBy(8.dp),
												verticalAlignment = Alignment.CenterVertically,
											) {
												if (episodeCount != null && episodeCount > 0) {
													Box(
														modifier = Modifier
															.clip(RoundedCornerShape(999.dp))
															.background(JellyfinTheme.colorScheme.badge)
															.padding(horizontal = 8.dp, vertical = 4.dp),
													) {
														Text(
															text = stringResource(
																R.string.jellyseerr_episodes_count,
																episodeCount,
															),
															color = JellyfinTheme.colorScheme.onBadge,
															fontSize = 12.sp,
														)
													}
												}

												if (!season.airDate.isNullOrBlank()) {
													Text(
														text = season.airDate.take(10),
														color = JellyfinTheme.colorScheme.onBackground.copy(alpha = 0.7f),
														fontSize = 12.sp,
													)
												}
											}

											// Overview
											if (!season.overview.isNullOrBlank()) {
												Text(
													text = season.overview,
													color = JellyfinTheme.colorScheme.onBackground.copy(alpha = 0.8f),
													fontSize = 12.sp,
													maxLines = 3,
													overflow = TextOverflow.Ellipsis,
												)
											}

											Spacer(modifier = Modifier.weight(1f))

											// Action Button
											Button(
												onClick = {
													when {
														isAvailable -> {
															navigationRepository.navigate(
																org.jellyfin.androidtv.ui.navigation.Destinations.search(
																	selectedItem.title,
																),
															)
															showSeasonDialog = false
														}
														seasonRequested -> {
															// Bereits angefragt - keine Aktion (disabled)
														}
														else -> {
															viewModel.request(selectedItem, listOf(number))
															showSeasonDialog = false
														}
													}
												},
												enabled = !seasonRequested,
												colors = buttonColors,
												interactionSource = buttonInteraction,
												modifier = buttonModifier
													.border(
														width = if (buttonFocused) 3.dp else 0.dp,
														color = Color.White,
														shape = CircleShape
													),
											) {
												if (isAvailable) {
													Icon(
														imageVector = ImageVector.vectorResource(id = R.drawable.ic_play),
														contentDescription = stringResource(R.string.lbl_play),
													)
												} else {
													Text(
														text = buttonText,
														textAlign = TextAlign.Center,
													)
												}
											}
										}
									}

									// Expandable Episodes Toggle
									val rowInteractionSource = remember { MutableInteractionSource() }
									var rowFocused by remember { mutableStateOf(false) }
									val expandRowBackground = if (rowFocused) {
										JellyfinTheme.colorScheme.buttonFocused.copy(alpha = 0.5f)
									} else {
										Color.Transparent
									}

									Spacer(modifier = Modifier.height(8.dp))

									Row(
										modifier = Modifier
											.fillMaxWidth()
											.onFocusChanged { rowFocused = it.isFocused }
											.clickable(
												interactionSource = rowInteractionSource,
												indication = null,
											) {
												expandedSeasons[number] = !expanded
											}
											.background(expandRowBackground, RoundedCornerShape(8.dp))
											.padding(horizontal = 8.dp, vertical = 6.dp),
										horizontalArrangement = Arrangement.spacedBy(8.dp),
										verticalAlignment = Alignment.CenterVertically,
									) {
										val indicatorText = if (expanded) "▾" else "▸"
										Text(
											text = indicatorText,
											color = JellyfinTheme.colorScheme.onBackground,
											fontSize = 16.sp,
										)
										Text(
											text = if (expanded) "Episoden ausblenden" else "Episoden anzeigen",
											color = JellyfinTheme.colorScheme.onBackground,
											fontSize = 14.sp,
										)
									}
								}

								if (expanded) {
									LaunchedEffect(expanded, seasonKey) {
										if (expanded) {
											viewModel.loadSeasonEpisodes(selectedItem.id, number)
										}
									}

									val episodes = state.seasonEpisodes[seasonKey]
									val isLoadingEpisodes = state.loadingSeasonKeys.contains(seasonKey)
									val seasonError = state.seasonErrors[seasonKey]

									Column(
										modifier = Modifier
											.fillMaxWidth()
											.padding(start = 32.dp, end = 4.dp),
										verticalArrangement = Arrangement.spacedBy(8.dp),
									) {
										when {
											isLoadingEpisodes -> {
												Text(
													text = stringResource(R.string.loading),
													color = JellyfinTheme.colorScheme.onBackground,
												)
											}
											!seasonError.isNullOrBlank() -> {
												Text(
													text = seasonError,
													color = Color.Red,
												)
											}
											episodes.isNullOrEmpty() -> {
												Text(
													text = stringResource(R.string.jellyseerr_no_episodes),
													color = JellyfinTheme.colorScheme.onBackground,
												)
											}
											else -> {
												episodes.forEach { episode ->
													val episodeInteraction = remember(episode.id) { MutableInteractionSource() }
													val episodeFocused by episodeInteraction.collectIsFocusedAsState()
													val episodeBackground = if (episodeFocused) {
														JellyfinTheme.colorScheme.buttonFocused.copy(alpha = 0.3f)
													} else {
														Color.Transparent
													}

													JellyseerrEpisodeRow(
														episode = episode,
														modifier = Modifier
															.fillMaxWidth()
															.padding(vertical = 4.dp)
															.focusable(interactionSource = episodeInteraction),
														backgroundColor = episodeBackground,
													)
												}
											}
										}
									}
								}
							}
						}

						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.End,
						) {
							// "Alle anfragen" Button nur anzeigen wenn es noch nicht angefragte Staffeln gibt
							val unrequestedSeasons = availableSeasons.filter { it.status == null }

							if (unrequestedSeasons.isNotEmpty()) {
								val requestAllInteraction = remember { MutableInteractionSource() }
								val requestAllFocused by requestAllInteraction.collectIsFocusedAsState()

								Button(
									onClick = {
										val seasonsToRequest = unrequestedSeasons.map { it.seasonNumber }

										if (seasonsToRequest.isNotEmpty()) {
											viewModel.request(selectedItem, seasonsToRequest)
										}
										showSeasonDialog = false
									},
									colors = ButtonDefaults.colors(
										containerColor = Color(0xFF9933CC),
										contentColor = Color.White,
										focusedContainerColor = Color(0xFFDD66FF),
										focusedContentColor = Color.Black,
									),
									interactionSource = requestAllInteraction,
									modifier = Modifier.border(
										width = if (requestAllFocused) 3.dp else 0.dp,
										color = Color.White,
										shape = CircleShape
									),
								) {
									Text(text = stringResource(R.string.jellyseerr_request_all_seasons))
								}
							}
						}
					}
				}

				// Fokus auf ersten Button setzen wenn Dialog geöffnet wird
				LaunchedEffect(Unit) {
					kotlinx.coroutines.delay(100)
					firstButtonFocusRequester.requestFocus()
				}
			}
		}
	}
}

@Composable
private fun JellyseerrContent(
	viewModel: JellyseerrViewModel = koinViewModel(),
	onShowSeasonDialog: () -> Unit,
	firstCastFocusRequester: FocusRequester,
) {
	val state by viewModel.uiState.collectAsState()
	val keyboardController = LocalSoftwareKeyboardController.current
	val searchFocusRequester = remember { FocusRequester() }
	val allTrendsListState = rememberLazyListState()
	val sectionSpacing = 10.dp
	val sectionInnerSpacing = 12.dp
	val sectionTitleFontSize = 26.sp
	val itemFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
	val viewAllFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
	val recentRequestsFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }

	LaunchedEffect(
		state.selectedItem,
		state.showAllTrendsGrid,
		state.lastFocusedItemId,
		state.lastFocusedViewAllKey,
	) {
		if (state.selectedItem == null && !state.showAllTrendsGrid) {
			val itemId = state.lastFocusedItemId
			if (itemId != null) {
				delay(100)
				itemFocusRequesters[itemId]?.requestFocus()
			} else {
				val viewAllKey = state.lastFocusedViewAllKey
				if (viewAllKey != null) {
					delay(100)
					viewAllFocusRequesters[viewAllKey]?.requestFocus()
				}
			}
		}
	}

	val focusRequesterForItem: (Int) -> FocusRequester = { id ->
		itemFocusRequesters.getOrPut(id) { FocusRequester() }
	}

	val focusRequesterForViewAll: (String) -> FocusRequester = { key ->
		viewAllFocusRequesters.getOrPut(key) { FocusRequester() }
	}

	BackHandler(enabled = state.selectedItem != null || state.showAllTrendsGrid || state.selectedPerson != null) {
		when {
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
			onShowSeasonDialog = onShowSeasonDialog,
			firstCastFocusRequester = firstCastFocusRequester,
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

			val shouldShowError = state.errorMessage?.contains("HTTP 400", ignoreCase = true) != true
			if (state.errorMessage != null && shouldShowError) {
				Text(
					text = stringResource(R.string.jellyseerr_error_prefix, state.errorMessage ?: ""),
					color = Color.Red,
					modifier = Modifier.padding(bottom = 16.dp),
				)
			}

			if (state.showAllTrendsGrid) {
				Text(text = stringResource(state.discoverCategory.titleResId), color = Color.White, fontSize = sectionTitleFontSize)

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
									.padding(vertical = 15.dp),
							) {
								for (item in rowItems) {
									JellyseerrSearchCard(
										item = item,
										onClick = { viewModel.showDetailsForItem(item) },
										focusRequester = focusRequesterForItem(item.id),
										onFocus = { viewModel.updateLastFocusedItem(item.id) },
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
				Text(text = stringResource(titleRes), color = JellyfinTheme.colorScheme.onBackground, fontSize = sectionTitleFontSize)

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
					val focusRequester = FocusRequester()
					val listState = rememberLazyListState(
						initialFirstVisibleItemIndex = state.scrollPositions["discover"]?.index ?: 0,
						initialFirstVisibleItemScrollOffset = state.scrollPositions["discover"]?.offset ?: 0,
					)

					// Speichere Scroll-Position wenn sich der Zustand ändert
					LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
						if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
							viewModel.saveScrollPosition(
								"discover",
								listState.firstVisibleItemIndex,
								listState.firstVisibleItemScrollOffset
							)
						}
					}

					LazyRow(
						state = listState,
						horizontalArrangement = Arrangement.spacedBy(12.dp),
						contentPadding = PaddingValues(horizontal = 24.dp),
						modifier = Modifier
							.fillMaxWidth()
							.height(300.dp)
							.padding(top = 15.dp),
					) {
						val maxIndex = baseResults.lastIndex
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
										focusRequester = focusRequesterForViewAll(VIEW_ALL_TRENDING),
										onFocus = { viewModel.updateLastFocusedViewAll(VIEW_ALL_TRENDING) },
									)
								}
							}
						}
					}

				if (
					state.query.isBlank() &&
					state.selectedItem == null &&
					state.lastFocusedItemId == null &&
					state.lastFocusedViewAllKey == null &&
					!state.showAllTrendsGrid
				) {
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
					fontSize = sectionTitleFontSize,
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

						val popularListState = rememberLazyListState(
							initialFirstVisibleItemIndex = state.scrollPositions["popular"]?.index ?: 0,
							initialFirstVisibleItemScrollOffset = state.scrollPositions["popular"]?.offset ?: 0,
						)

						LaunchedEffect(popularListState.firstVisibleItemIndex, popularListState.firstVisibleItemScrollOffset) {
							if (popularListState.firstVisibleItemIndex > 0 || popularListState.firstVisibleItemScrollOffset > 0) {
								viewModel.saveScrollPosition(
									"popular",
									popularListState.firstVisibleItemIndex,
									popularListState.firstVisibleItemScrollOffset
								)
							}
						}

						LazyRow(
							state = popularListState,
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
										focusRequester = focusRequesterForItem(item.id),
										onFocus = { viewModel.updateLastFocusedItem(item.id) },
									)
									}
								index == maxIndex + 1 -> {
									JellyseerrViewAllCard(
										onClick = { viewModel.showAllPopularMovies() },
										focusRequester = focusRequesterForViewAll(VIEW_ALL_POPULAR_MOVIES),
										onFocus = { viewModel.updateLastFocusedViewAll(VIEW_ALL_POPULAR_MOVIES) },
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
					fontSize = sectionTitleFontSize,
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
											focusRequester = focusRequesterForItem(item.id),
											onFocus = { viewModel.updateLastFocusedItem(item.id) },
										)
									}
								index == maxIndex + 1 -> {
									JellyseerrViewAllCard(
										onClick = { viewModel.showAllPopularTv() },
										focusRequester = focusRequesterForViewAll(VIEW_ALL_POPULAR_TV),
										onFocus = { viewModel.updateLastFocusedViewAll(VIEW_ALL_POPULAR_TV) },
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
					fontSize = sectionTitleFontSize,
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
											focusRequester = focusRequesterForItem(item.id),
											onFocus = { viewModel.updateLastFocusedItem(item.id) },
										)
									}
								index == maxIndex + 1 -> {
									JellyseerrViewAllCard(
										onClick = { viewModel.showAllUpcomingMovies() },
										focusRequester = focusRequesterForViewAll(VIEW_ALL_UPCOMING_MOVIES),
										onFocus = { viewModel.updateLastFocusedViewAll(VIEW_ALL_UPCOMING_MOVIES) },
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
					fontSize = sectionTitleFontSize,
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
											focusRequester = focusRequesterForItem(item.id),
											onFocus = { viewModel.updateLastFocusedItem(item.id) },
										)
									}
									index == maxIndex + 1 -> {
										JellyseerrViewAllCard(
											onClick = { viewModel.showAllUpcomingTv() },
											focusRequester = focusRequesterForViewAll(VIEW_ALL_UPCOMING_TV),
											onFocus = { viewModel.updateLastFocusedViewAll(VIEW_ALL_UPCOMING_TV) },
										)
									}
								}
							}
						}
					}
				}

				// Bisherige Anfragen (nur eigene Anfragen)
				if (state.selectedItem == null && state.selectedPerson == null && state.query.isBlank()) {
					Spacer(modifier = Modifier.size(sectionSpacing))

					Text(
						text = stringResource(R.string.jellyseerr_recent_requests_title),
					color = JellyfinTheme.colorScheme.onBackground,
					fontSize = sectionTitleFontSize,
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
								.height(300.dp),
						) {
							items(state.recentRequests) { item ->
								val focusRequester = recentRequestsFocusRequesters.getOrPut(item.id) { FocusRequester() }
								JellyseerrSearchCard(
									item = item,
									onClick = { viewModel.showDetailsForItem(item) },
									focusRequester = focusRequester,
									onFocus = { }, // Kein lastFocusedItem Update für Recent Requests
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
	private fun JellyseerrEpisodeRow(
		episode: JellyseerrEpisode,
		modifier: Modifier = Modifier,
		backgroundColor: Color = Color.Transparent,
	) {
	val titleParts = buildList {
		episode.episodeNumber?.let { add(stringResource(R.string.lbl_episode_number, it)) }
		if (!episode.name.isNullOrBlank()) add(episode.name)
	}
	val titleText = titleParts.joinToString(" – ").ifBlank { stringResource(R.string.jellyseerr_episode_title_missing) }

	Row(
		modifier = modifier
			.background(backgroundColor, RoundedCornerShape(8.dp))
			.padding(8.dp),
		verticalAlignment = Alignment.Top,
		horizontalArrangement = Arrangement.Start,
	) {
		val thumbnailModifier = Modifier
			.size(110.dp)
			.clip(RoundedCornerShape(8.dp))

		if (!episode.imageUrl.isNullOrBlank()) {
			AsyncImage(
				modifier = thumbnailModifier,
				url = episode.imageUrl,
				aspectRatio = 16f / 9f,
				scaleType = ImageView.ScaleType.CENTER_CROP,
			)
		} else {
			Box(
				modifier = thumbnailModifier
					.background(JellyfinTheme.colorScheme.popover),
				contentAlignment = Alignment.Center,
			) {
				Text(
					text = stringResource(R.string.jellyseerr_episode_title_missing),
					color = JellyfinTheme.colorScheme.onBackground,
					fontSize = 12.sp,
					textAlign = TextAlign.Center,
				)
			}
		}

		Spacer(modifier = Modifier.size(12.dp))

		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(6.dp),
		) {
			Text(
				text = titleText,
				color = JellyfinTheme.colorScheme.onBackground,
			)

			if (!episode.overview.isNullOrBlank()) {
				Text(
					text = episode.overview,
					color = JellyfinTheme.colorScheme.onBackground,
					maxLines = 3,
					overflow = TextOverflow.Ellipsis,
				)
			}

			if (episode.isMissing) {
				Box(
					modifier = Modifier
						.clip(RoundedCornerShape(999.dp))
						.background(JellyfinTheme.colorScheme.badge)
						.padding(horizontal = 8.dp, vertical = 2.dp),
				) {
					Text(
						text = stringResource(R.string.jellyseerr_episode_missing_badge),
						color = JellyfinTheme.colorScheme.onBadge,
						fontSize = 12.sp,
					)
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

        Spacer(modifier = Modifier.size(32.dp))

        LazyColumn {
            val rows = credits.chunked(5)
            items(rows.size) { rowIndex ->
                val rowItems = rows[rowIndex]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
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
	focusRequester: FocusRequester? = null,
	onFocus: (() -> Unit)? = null,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()

	LaunchedEffect(isFocused) {
		if (isFocused) {
			onFocus?.invoke()
		}
	}

	Box(
		modifier = Modifier
			.width(80.dp)
			.height(200.dp)
			.padding(vertical = 4.dp),
		contentAlignment = Alignment.Center,
	) {
		Button(
			onClick = onClick,
			interactionSource = interactionSource,
			colors = ButtonDefaults.colors(
				containerColor = Color.Transparent,
				contentColor = Color.White,
				focusedContainerColor = Color.White,
				focusedContentColor = Color.Black,
			),
			modifier = if (focusRequester != null) {
				Modifier.focusRequester(focusRequester)
			} else {
				Modifier
			},
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
	focusRequester: FocusRequester? = null,
	onFocus: (() -> Unit)? = null,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val scale = if (isFocused) 1.1f else 1f

	LaunchedEffect(isFocused) {
		if (isFocused) {
			onFocus?.invoke()
		}
	}

	Column(
		modifier = modifier
			.width(150.dp)
			.fillMaxSize()
			.clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
			.then(
				if (focusRequester != null) {
					Modifier.focusRequester(focusRequester)
				} else {
					Modifier
				}
			)
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
					width = if (isFocused) 4.dp else 1.dp,
					color = if (isFocused) Color.White else Color(0xFF888888),
					shape = RoundedCornerShape(12.dp),
				),
		) {
			if (item.posterPath.isNullOrBlank()) {
				// Placeholder wenn kein Poster verfügbar
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(Color(0xFF333333)),
					contentAlignment = Alignment.Center,
				) {
					androidx.compose.foundation.Image(
						imageVector = ImageVector.vectorResource(id = R.drawable.ic_clapperboard),
						contentDescription = null,
						modifier = Modifier.size(48.dp),
						colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF888888)),
					)
				}
			} else {
				AsyncImage(
					modifier = Modifier.fillMaxSize(),
					url = item.posterPath,
					aspectRatio = 2f / 3f,
					scaleType = ImageView.ScaleType.CENTER_CROP,
				)
			}

			val hasPendingRequest = item.requestStatus != null && item.requestStatus != 5

			when {
				hasPendingRequest -> {
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
				item.isPartiallyAvailable -> {
					Box(
						modifier = Modifier
							.align(Alignment.TopEnd)
							.padding(6.dp)
							.clip(RoundedCornerShape(999.dp))
							.background(Color(0xFF00C800)),
					) {
						androidx.compose.foundation.Image(
							imageVector = ImageVector.vectorResource(id = R.drawable.ic_decrease),
							contentDescription = null,
							modifier = Modifier
								.padding(4.dp)
								.size(16.dp),
						)
					}
				}
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
					width = if (isFocused) 4.dp else 1.dp,
					color = if (isFocused) Color.White else Color(0xFF888888),
					shape = RoundedCornerShape(12.dp),
				),
		) {
			if (request.posterPath.isNullOrBlank()) {
				// Placeholder wenn kein Poster verfügbar
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(Color(0xFF333333)),
					contentAlignment = Alignment.Center,
				) {
					androidx.compose.foundation.Image(
						imageVector = ImageVector.vectorResource(id = R.drawable.ic_clapperboard),
						contentDescription = null,
						modifier = Modifier.size(48.dp),
						colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF888888)),
					)
				}
			} else {
				AsyncImage(
					modifier = Modifier.fillMaxSize(),
					url = request.posterPath,
					aspectRatio = 2f / 3f,
					scaleType = ImageView.ScaleType.CENTER_CROP,
				)
			}

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
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.padding(horizontal = 4.dp),
		)
	}
}

@Composable
private fun JellyseerrCastRow(
	cast: List<JellyseerrCast>,
	onCastClick: (JellyseerrCast) -> Unit,
	firstCastFocusRequester: FocusRequester? = null,
) {
	val displayCast = cast.take(10)

	LazyRow(
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 4.dp),
	) {
		itemsIndexed(displayCast) { index, person ->
			JellyseerrCastCard(
				person = person,
				onClick = { onCastClick(person) },
				focusRequester = if (index == 0) firstCastFocusRequester else null,
			)
		}
	}
}

@Composable
private fun JellyseerrCastCard(
	person: JellyseerrCast,
	onClick: () -> Unit,
	focusRequester: FocusRequester? = null,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()
	val scale = if (isFocused) 1.05f else 1f

	val modifier = Modifier
		.width(120.dp)
		.padding(vertical = 4.dp)
		.clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
		.focusable(interactionSource = interactionSource)
		.graphicsLayer(
			scaleX = scale,
			scaleY = scale,
		)

	val modifierWithFocus = if (focusRequester != null) {
		modifier.focusRequester(focusRequester)
	} else {
		modifier
	}

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = modifierWithFocus,
	) {
		Box(
			modifier = Modifier
				.size(80.dp)
				.clip(CircleShape)
				.border(
					width = if (isFocused) 4.dp else 1.dp,
					color = if (isFocused) Color.White else Color(0xFF888888),
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
	onShowSeasonDialog: () -> Unit,
	firstCastFocusRequester: FocusRequester = remember { FocusRequester() },
) {
	val requestButtonFocusRequester = remember { FocusRequester() }
	val navigationRepository = koinInject<org.jellyfin.androidtv.ui.navigation.NavigationRepository>()
	val isTv = item.mediaType == "tv"

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
					val posterUrl = details?.posterPath ?: item.posterPath
					if (posterUrl.isNullOrBlank()) {
						// Placeholder wenn kein Poster verfügbar
						Box(
							modifier = Modifier
								.fillMaxSize()
								.background(Color(0xFF333333)),
							contentAlignment = Alignment.Center,
						) {
							androidx.compose.foundation.Image(
								imageVector = ImageVector.vectorResource(id = R.drawable.ic_clapperboard),
								contentDescription = null,
								modifier = Modifier.size(64.dp),
								colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF888888)),
							)
						}
					} else {
						AsyncImage(
							modifier = Modifier.fillMaxSize(),
							url = posterUrl,
							aspectRatio = 2f / 3f,
							scaleType = ImageView.ScaleType.CENTER_CROP,
						)
					}
				}

				val isRequested = item.isRequested
				val isAvailable = item.isAvailable
				val isPartiallyAvailable = item.isPartiallyAvailable && !isAvailable

				val requestButtonInteraction = remember { MutableInteractionSource() }
				val requestButtonFocused by requestButtonInteraction.collectIsFocusedAsState()

				val buttonColors = when {
					isAvailable -> ButtonDefaults.colors(
						containerColor = Color(0xFF00C853),
						contentColor = Color.White,
						focusedContainerColor = Color(0xFF64DD17),
						focusedContentColor = Color.White,
					)
					isPartiallyAvailable -> ButtonDefaults.colors(
						containerColor = Color(0xFF0097A7),
						contentColor = Color.White,
						focusedContainerColor = Color(0xFF00ACC1),
						focusedContentColor = Color.White,
					)
					isRequested -> ButtonDefaults.colors(
						containerColor = Color(0xFFDD8800),
						contentColor = Color.Black,
						focusedContainerColor = Color(0xFFFFBB00),
						focusedContentColor = Color.Black,
					)
					else -> ButtonDefaults.colors(
						containerColor = Color(0xFF9933CC),
						contentColor = Color.White,
						focusedContainerColor = Color(0xFFDD66FF),
						focusedContentColor = Color.Black,
					)
				}

				val buttonText = when {
					isAvailable -> stringResource(R.string.lbl_play)
					isPartiallyAvailable -> stringResource(R.string.jellyseerr_partially_available_label)
					isRequested -> stringResource(R.string.jellyseerr_requested_label)
					else -> stringResource(R.string.jellyseerr_request_button)
				}

				Box(
					contentAlignment = Alignment.Center,
				) {
					Button(
						onClick = {
							when {
								isAvailable -> {
									navigationRepository.navigate(
										org.jellyfin.androidtv.ui.navigation.Destinations.search(item.title),
									)
								}
								isTv -> {
									// Serie: Dialog öffnen
									onShowSeasonDialog()
								}
								else -> {
									// Film: direkte Anfrage
									onRequestClick(null)
								}
							}
						},
						enabled = !isRequested || isTv || isAvailable,
						colors = buttonColors,
						interactionSource = requestButtonInteraction,
						modifier = Modifier
							.width(200.dp)
							.focusRequester(requestButtonFocusRequester)
							.border(
								width = if (requestButtonFocused) 3.dp else 0.dp,
								color = Color.White,
								shape = CircleShape
							),
					) {
						Box(
							modifier = Modifier.fillMaxWidth(),
							contentAlignment = Alignment.Center
						) {
							if (isAvailable) {
								androidx.compose.foundation.Image(
									imageVector = ImageVector.vectorResource(id = R.drawable.ic_play),
									contentDescription = buttonText,
									modifier = Modifier.size(24.dp)
								)
							} else {
								Text(text = buttonText)
							}
						}
					}
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
						firstCastFocusRequester = firstCastFocusRequester,
					)
				}

				Spacer(modifier = Modifier.size(16.dp))
			}
		}
	}

	LaunchedEffect(Unit) {
		kotlinx.coroutines.delay(100)
		requestButtonFocusRequester.requestFocus()
	}
}
