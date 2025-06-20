package org.jellyfin.androidtv.ui.browsing.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.composable.tv.ImmersiveBackground
import org.jellyfin.androidtv.ui.composable.tv.ImmersiveListLayout
import org.jellyfin.androidtv.ui.composable.tv.ImmersiveListSection
import org.jellyfin.androidtv.ui.composable.tv.MediaCard
import org.jellyfin.androidtv.ui.theme.JellyfinColors
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

/**
 * Series detail screen showing seasons using the immersive list pattern
 * This screen displays all seasons for a specific TV series
 */
@Composable
fun ComposeSeriesScreen(
	seriesId: UUID,
	modifier: Modifier = Modifier,
	viewModel: ComposeSeriesViewModel = koinViewModel(),
) {
	val uiState by viewModel.uiState.collectAsState()

	// Load series data when the screen is first composed
	LaunchedEffect(seriesId) {
		viewModel.loadSeriesData(seriesId)
	}

	Box(
		modifier = modifier.fillMaxSize(),
	) {
		when {
			uiState.isLoading -> {
				LoadingState()
			}
			uiState.error != null -> {
				ErrorState(error = uiState.error!!)
			}
			uiState.sections.isEmpty() -> {
				EmptyState()
			}
			else -> {
				ContentState(
					uiState = uiState,
					onItemClick = viewModel::onItemClick,
					onItemFocused = viewModel::onItemFocused,
					getItemImageUrl = viewModel::getItemImageUrl,
					getItemBackdropUrl = viewModel::getItemBackdropUrl,
					getItemLogoUrl = viewModel::getItemLogoUrl,
				)
			}
		}
	}
}

@Composable
private fun LoadingState(
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			CircularProgressIndicator()
			Text(
				text = "Loading seasons...",
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier.padding(top = 16.dp),
			)
		}
	}
}

@Composable
private fun ErrorState(
	error: String,
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Text(
			text = "Error: $error",
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.error,
		)
	}
}

@Composable
private fun EmptyState(
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Text(
			text = "No seasons found",
			style = MaterialTheme.typography.bodyLarge,
		)
	}
}

@Composable
private fun ContentState(
	uiState: SeriesUiState,
	onItemClick: (org.jellyfin.sdk.model.api.BaseItemDto) -> Unit,
	onItemFocused: (org.jellyfin.sdk.model.api.BaseItemDto?) -> Unit,
	getItemImageUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String?,
	getItemBackdropUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String?,
	getItemLogoUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String?,
	modifier: Modifier = Modifier,
) {
	SeriesDetailImmersiveList(
		series = uiState.series,
		sections = uiState.sections,
		onItemClick = onItemClick,
		onItemFocus = onItemFocused,
		getItemImageUrl = getItemImageUrl,
		getItemBackdropUrl = getItemBackdropUrl,
		getItemLogoUrl = getItemLogoUrl,
		modifier = modifier,
	)
}

/**
 * Focused item overlay showing details of the currently focused item
 */
@Composable
private fun FocusedItemOverlay(
	item: org.jellyfin.sdk.model.api.BaseItemDto?,
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier
			.background(
				Brush.verticalGradient(
					colors = listOf(
						Color.Black.copy(alpha = 0.7f),
						Color.Black.copy(alpha = 0.4f),
						Color.Transparent,
					),
				),
			)
			.padding(horizontal = 48.dp),
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 32.dp),
		) {
			item?.let { focusedItem ->
				// Title
				Text(
					text = focusedItem.name ?: "Unknown",
					style = MaterialTheme.typography.displayMedium.copy(
						fontWeight = FontWeight.Bold,
						fontSize = 42.sp,
					),
					color = Color.White,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.padding(bottom = 12.dp),
				)

				// Metadata row
				Row(
					horizontalArrangement = Arrangement.spacedBy(16.dp),
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier.padding(bottom = 8.dp),
				) {
					// Year
					focusedItem.productionYear?.let { year ->
						Text(
							text = year.toString(),
							style = MaterialTheme.typography.titleLarge,
							color = Color.White.copy(alpha = 0.7f),
						)
					}

					// Runtime
					focusedItem.runTimeTicks?.let { ticks ->
						val minutes = (ticks / 10_000_000) / 60
						if (minutes > 0) {
							Text(
								text = "${minutes}m",
								style = MaterialTheme.typography.titleLarge,
								color = Color.White.copy(alpha = 0.7f),
							)
						}
					}
				}

				// Overview
				focusedItem.overview?.let { overview ->
					Text(
						text = overview,
						style = MaterialTheme.typography.bodyLarge,
						color = Color.White.copy(alpha = 0.8f),
						maxLines = 3,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.padding(end = 400.dp),
					)
				}
			}
		}
	}
}

/**
 * Horizontal row of cards for seasons
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HorizontalCardRow(
	items: List<org.jellyfin.sdk.model.api.BaseItemDto>,
	onItemClick: (org.jellyfin.sdk.model.api.BaseItemDto) -> Unit,
	getItemImageUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String?,
	modifier: Modifier = Modifier,
) {
	LazyRow(
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		modifier = modifier.height(180.dp),
		contentPadding = PaddingValues(horizontal = 48.dp),
	) {
		items(items) { item ->
			MediaCard(
				item = item,
				imageUrl = getItemImageUrl(item),
				onClick = { onItemClick(item) },
				width = 240.dp,
				aspectRatio = 16f / 9f, // Horizontal aspect ratio for seasons
				showTitle = true,
			)
		}
	}
}

/**
 * Horizontal row of cast cards (single row)
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CastRow(
        items: List<org.jellyfin.sdk.model.api.BaseItemDto>,
        onItemClick: (org.jellyfin.sdk.model.api.BaseItemDto) -> Unit,
        getItemImageUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String?,
        modifier: Modifier = Modifier,
) {
	LazyRow(
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		modifier = modifier.height(160.dp),
		contentPadding = PaddingValues(horizontal = 48.dp),
	) {
                items(items) { item ->
                        MediaCard(
                                item = item,
                                imageUrl = getItemImageUrl(item),
                                onClick = {
                                        onItemClick(item)
                                },
                                width = 160.dp * 2 / 3,
                                aspectRatio = 2f / 3f,
                                showTitle = false,
                        )
                }
        }
}

/**
 * Clean series detail layout with proper sections
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SeriesDetailImmersiveList(
	series: org.jellyfin.sdk.model.api.BaseItemDto?,
	sections: List<ImmersiveListSection>,
	modifier: Modifier = Modifier,
	onItemClick: (org.jellyfin.sdk.model.api.BaseItemDto) -> Unit = {},
	onItemFocus: (org.jellyfin.sdk.model.api.BaseItemDto) -> Unit = {},
	getItemImageUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String? = { null },
	getItemBackdropUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String? = { null },
	getItemLogoUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String? = { null },
) {
	var globalFocusedItem by remember { mutableStateOf<org.jellyfin.sdk.model.api.BaseItemDto?>(null) }

	// Use series as background when no item is focused, otherwise use focused item
	val backgroundItem = globalFocusedItem ?: series

	LaunchedEffect(globalFocusedItem) {
		if (globalFocusedItem != null) {
			delay(150)
			onItemFocus(globalFocusedItem!!)
		}
	}

	Box(modifier = modifier.fillMaxSize()) {
		// Background
		ImmersiveBackground(
			item = backgroundItem,
			getBackdropUrl = getItemBackdropUrl,
		)

		// Content with clean layout
		LazyColumn(
			verticalArrangement = Arrangement.spacedBy(32.dp),
			contentPadding = PaddingValues(bottom = 48.dp),
			modifier = Modifier
				.fillMaxSize()
				.background(
					Brush.verticalGradient(
						colors = listOf(
							Color.Black.copy(alpha = 0.4f),
							Color.Black.copy(alpha = 0.6f),
							Color.Black.copy(alpha = 0.8f),
						),
					),
				),
		) {
			// Series information header - only show when no item is focused
			item {
				if (globalFocusedItem == null) {
					SeriesInformationOverlay(
						series = series,
						focusedItem = null,
						getItemLogoUrl = getItemLogoUrl,
						modifier = Modifier
							.fillMaxWidth()
							.height(320.dp),
					)
				} else {
					// Show focused item info
					FocusedItemOverlay(
						item = globalFocusedItem,
						modifier = Modifier
							.fillMaxWidth()
							.height(280.dp),
					)
				}
			}

			// Sections with seasons/cast
			items(sections) { section ->
				Column(
					modifier = Modifier.fillMaxWidth(),
				) {
					// Section title
					Text(
						text = section.title,
						style = MaterialTheme.typography.headlineMedium.copy(
							fontWeight = FontWeight.Bold,
							fontSize = 28.sp,
						),
						color = Color.White,
						modifier = Modifier
							.padding(horizontal = 48.dp)
							.padding(bottom = 20.dp),
					)

					// Section content
					when (section.layout) {
						ImmersiveListLayout.HORIZONTAL_CARDS -> {
							HorizontalCardRow(
								items = section.items,
								onItemClick = onItemClick,
								getItemImageUrl = getItemImageUrl,
							)
						}
						ImmersiveListLayout.VERTICAL_GRID -> {
                                                        CastRow(
                                                                items = section.items,
                                                                onItemClick = onItemClick,
                                                                getItemImageUrl = getItemImageUrl,
                                                        )
						}
					}
				}
			}
		}
	}
}

/**
 * Series information overlay showing detailed metadata about the TV series
 */
@Composable
private fun SeriesInformationOverlay(
	series: org.jellyfin.sdk.model.api.BaseItemDto?,
	focusedItem: org.jellyfin.sdk.model.api.BaseItemDto?,
	getItemLogoUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String? = { null },
	modifier: Modifier = Modifier,
) {
	// Show series info when no item is focused, otherwise show focused item info
	val displayItem = focusedItem ?: series

	Box(
		modifier = modifier
			.background(
				Brush.verticalGradient(
					colors = listOf(
						Color.Black.copy(alpha = 0.7f),
						Color.Black.copy(alpha = 0.4f),
						Color.Transparent,
					),
					startY = 0f,
					endY = Float.POSITIVE_INFINITY,
				),
			)
			.padding(horizontal = 48.dp),
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 32.dp),
			verticalArrangement = Arrangement.Center,
		) {
			// Title
			displayItem?.let { item ->
				Text(
					text = item.name ?: "Unknown Title",
					style = MaterialTheme.typography.displayMedium.copy(
						fontWeight = FontWeight.Bold,
						fontSize = 48.sp,
					),
					color = Color.White,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier
						.padding(bottom = 12.dp, end = 380.dp),
				)

				// Series-specific metadata row
				Row(
					horizontalArrangement = Arrangement.spacedBy(24.dp),
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier.padding(bottom = 8.dp),
				) {
					// Production year range (for series) or year (for individual items)
					if (focusedItem == null && series != null) {
						// Show series year range
						val startYear = series.productionYear
						val endYear = series.endDate?.year
						val yearText = when {
							startYear != null && endYear != null && endYear != startYear -> "$startYear - $endYear"
							startYear != null && series.status?.lowercase() == "continuing" -> "$startYear - Present"
							startYear != null -> startYear.toString()
							else -> null
						}
						yearText?.let { year ->
							Text(
								text = year,
								style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
								color = Color.White.copy(alpha = 0.7f),
							)
						}
					} else {
						// Show focused item year
						item.productionYear?.let { year ->
							Text(
								text = year.toString(),
								style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
								color = Color.White.copy(alpha = 0.7f),
							)
						}
					}

					// Community rating (star rating)
					item.communityRating?.let { rating ->
						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.spacedBy(4.dp),
						) {
							Icon(
								painter = painterResource(R.drawable.ic_star),
								contentDescription = "Rating",
								tint = Color.Yellow,
								modifier = Modifier.size(20.dp),
							)
							Text(
								text = String.format("%.1f", rating),
								style = MaterialTheme.typography.titleLarge.copy(
									fontSize = 18.sp,
									fontWeight = FontWeight.Medium,
								),
								color = Color.White,
							)
						}
					}

					// Status (for series)
					if (focusedItem == null) {
						series?.status?.let { status ->
							Text(
								text = status,
								style = MaterialTheme.typography.titleLarge.copy(
									fontSize = 18.sp,
									fontWeight = FontWeight.Medium,
								),
								color = when (status.lowercase()) {
									"continuing", "ongoing" -> Color.Green
									"ended", "cancelled" -> Color.Red.copy(alpha = 0.8f)
									else -> Color.White.copy(alpha = 0.7f)
								},
								modifier = Modifier
									.background(
										color = Color.Black.copy(alpha = 0.6f),
										shape = RoundedCornerShape(6.dp),
									)
									.padding(horizontal = 12.dp, vertical = 4.dp),
							)
						}
					}

					// Episode count (for series)
					if (focusedItem == null) {
						series?.childCount?.let { count ->
							if (count > 0) {
								Text(
									text = "$count Episodes",
									style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
									color = Color.White.copy(alpha = 0.7f),
								)
							}
						}
					}

					// Runtime (for focused episodes)
					if (focusedItem != null) {
						item.runTimeTicks?.let { ticks ->
							val minutes = (ticks / 10_000_000) / 60
							if (minutes > 0) {
								Text(
									text = "${minutes}m",
									style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
									color = Color.White.copy(alpha = 0.7f),
								)
							}
						}
					}
				}

				// Genres row (for series when no item focused)
				if (focusedItem == null && !series?.genres.isNullOrEmpty()) {
					Row(
						horizontalArrangement = Arrangement.spacedBy(12.dp),
						modifier = Modifier.padding(bottom = 16.dp),
					) {
						series?.genres?.take(4)?.forEach { genre ->
							Text(
								text = genre,
								style = MaterialTheme.typography.bodyLarge.copy(
									fontSize = 16.sp,
									fontWeight = FontWeight.Medium,
								),
								color = JellyfinColors.Primary,
								modifier = Modifier
									.background(
										color = Color.Black.copy(alpha = 0.6f),
										shape = RoundedCornerShape(6.dp),
									)
									.padding(horizontal = 12.dp, vertical = 6.dp),
							)
						}
					}
				}

				// Overview/description
				item.overview?.let { overview ->
					Text(
						text = overview,
						style = MaterialTheme.typography.bodyLarge.copy(
							fontSize = 16.sp,
							lineHeight = 22.sp,
						),
						color = Color.White.copy(alpha = 0.8f),
						maxLines = if (focusedItem == null) 4 else 3, // More lines for series description
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.padding(bottom = 24.dp, end = 400.dp),
					)
				}
			}
		}

		// Logo positioned at top right
		displayItem?.let { item ->
			val logoUrl = getItemLogoUrl(item)
			if (logoUrl != null) {
				AsyncImage(
					model = logoUrl,
					contentDescription = "${item.name} logo",
					modifier = Modifier
						.align(Alignment.TopEnd)
						.padding(top = 100.dp, end = 60.dp)
						.height(120.dp)
						.widthIn(max = 380.dp),
					contentScale = ContentScale.Fit,
				)
			}
		}
	}
}
