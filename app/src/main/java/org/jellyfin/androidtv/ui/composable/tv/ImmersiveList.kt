package org.jellyfin.androidtv.ui.composable.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import org.jellyfin.androidtv.ui.theme.JellyfinColors
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaStreamType

/**
 * Layout type for the immersive list
 */
enum class ImmersiveListLayout {
	HORIZONTAL_CARDS,
	VERTICAL_GRID,
	CAST_ROW,
}

/**
 * Background mode for the immersive list
 */
enum class BackgroundMode {
	FOCUSED_ITEM,
	STATIC,
	NONE,
}

/**
 * Immersive List component following Android TV design guidelines
 * Features:
 * - Full-screen immersive background from focused item
 * - Content information overlay (title, description, metadata)
 * - Cards positioned in lower portion of screen
 * - Support for both horizontal rows and vertical grids
 * - Smooth focus animations and proper D-pad navigation
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ImmersiveList(
	title: String,
	items: List<BaseItemDto>,
	modifier: Modifier = Modifier,
	layout: ImmersiveListLayout = ImmersiveListLayout.HORIZONTAL_CARDS,
	backgroundMode: BackgroundMode = BackgroundMode.FOCUSED_ITEM,
	onItemClick: (BaseItemDto) -> Unit = {},
	onItemFocus: (BaseItemDto) -> Unit = {},
	getItemImageUrl: (BaseItemDto) -> String? = { null },
	getItemBackdropUrl: (BaseItemDto) -> String? = { null },
	getItemLogoUrl: (BaseItemDto) -> String? = { null },
) {
	var focusedItem by remember { mutableStateOf<BaseItemDto?>(items.firstOrNull()) }

	// Initialize with first item for immediate background
	LaunchedEffect(items) {
		if (focusedItem == null && items.isNotEmpty()) {
			focusedItem = items.first()
			onItemFocus(items.first())
		}
	}

	// Manage background updates with debouncing
	LaunchedEffect(focusedItem) {
		if (focusedItem != null) {
			// Small delay to prevent rapid background changes during navigation
			delay(150)
			onItemFocus(focusedItem!!)
		}
	}

	Box(modifier = modifier.fillMaxSize()) {
		// 1. Full-screen immersive background
		when (backgroundMode) {
			BackgroundMode.FOCUSED_ITEM -> {
				ImmersiveBackground(
					item = focusedItem,
					getBackdropUrl = getItemBackdropUrl,
				)
			}
			BackgroundMode.STATIC -> {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(Color.Black.copy(alpha = 0.3f)),
				)
			}
			BackgroundMode.NONE -> {
				// No background
			}
		}

		// 2. Content layout with information overlay and cards
		Column(
			modifier = Modifier.fillMaxSize(),
		) {
			// Content information overlay (top portion)
			ContentInformationOverlay(
				focusedItem = focusedItem,
				sectionTitle = title,
				getItemLogoUrl = getItemLogoUrl,
				modifier = Modifier
					.fillMaxWidth()
					.weight(0.6f), // Takes about 60% of screen height
			)

			// Cards grid (bottom portion)
			CardsSection(
				items = items,
				layout = layout,
				focusedItem = focusedItem,
				onItemClick = onItemClick,
				onItemFocus = { item -> focusedItem = item },
				getItemImageUrl = getItemImageUrl,
				modifier = Modifier
					.fillMaxWidth()
					.weight(0.4f), // Takes about 40% of screen height
			)
		}
	}
}

/**
 * Immersive background that shows backdrop of focused item
 */
@Composable
internal fun ImmersiveBackground(
	item: BaseItemDto?,
	getBackdropUrl: (BaseItemDto) -> String?,
) {
	AnimatedVisibility(
		visible = item != null,
		enter = fadeIn(tween(500)),
		exit = fadeOut(tween(300)),
	) {
		if (item != null) {
			val backdropUrl = getBackdropUrl(item)
			if (backdropUrl != null) {
				AsyncImage(
					model = backdropUrl,
					contentDescription = null,
					modifier = Modifier.fillMaxSize(),
					contentScale = ContentScale.Crop,
					alpha = 0.7f,
				)
			}
		}
	}
}

/**
 * Content information overlay showing details about the focused item
 * Positioned in the upper portion of the screen
 */
@Composable
private fun ContentInformationOverlay(
	focusedItem: BaseItemDto?,
	sectionTitle: String,
	getItemLogoUrl: (BaseItemDto) -> String? = { null },
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier
			.background(
				Brush.verticalGradient(
					colors = listOf(
						Color.Black.copy(alpha = 0.6f),
						Color.Black.copy(alpha = 0.3f),
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
			// Section title
			Text(
				text = sectionTitle,
				style = MaterialTheme.typography.headlineLarge.copy(
					fontWeight = FontWeight.Bold,
					fontSize = 28.sp,
				),
				color = Color.White.copy(alpha = 0.8f),
				modifier = Modifier.padding(bottom = 16.dp),
			)

			// Focused item information
			focusedItem?.let { item ->
				// Item title (without logo in the row)
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
						.padding(bottom = 8.dp, end = 380.dp), // Reserve space for logo on the right
				)

				// Ratings and metadata row
				Row(
					horizontalArrangement = Arrangement.spacedBy(24.dp),
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier.padding(bottom = 16.dp), // Increased spacing for better breathing room
				) {
					// Production year
					item.productionYear?.let { year ->
						Text(
							text = year.toString(),
							style = MaterialTheme.typography.titleLarge.copy(
								fontSize = 20.sp,
							),
							color = Color.White.copy(alpha = 0.7f),
						)
					}

					// Community rating (star rating)
					item.communityRating?.let { rating ->
						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.spacedBy(4.dp),
						) {
							Icon(
								painter = painterResource(R.drawable.ic_star),
								contentDescription = "Community Rating",
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

					// Critic rating (Rotten Tomatoes)
					item.criticRating?.let { rating ->
						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.spacedBy(4.dp),
						) {
							Icon(
								painter = when {
									rating >= 60f -> painterResource(R.drawable.ic_rt_fresh)
									else -> painterResource(R.drawable.ic_rt_rotten)
								},
								contentDescription = "Critic Rating",
								tint = Color.Unspecified,
								modifier = Modifier.size(20.dp),
							)
							Text(
								text = "${rating.toInt()}%",
								style = MaterialTheme.typography.titleLarge.copy(
									fontSize = 18.sp,
									fontWeight = FontWeight.Medium,
								),
								color = Color.White,
							)
						}
					}

					// Status (for TV shows - continuing/ended)
					item.status?.let { status ->
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
						)
					}

					// Runtime (for episodes/movies)
					item.runTimeTicks?.let { ticks ->
						val minutes = (ticks / 10_000_000) / 60 // Convert from ticks to minutes
						if (minutes > 0) {
							Text(
								text = "${minutes}m",
								style = MaterialTheme.typography.titleLarge.copy(
									fontSize = 18.sp,
								),
								color = Color.White.copy(alpha = 0.7f),
							)
						}
					}

					// Video quality indicator (for episodes/movies)
					item.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }?.let { videoStream ->
						videoStream.height?.let { height ->
							val quality = when {
								height >= 2160 -> "4K"
								height >= 1080 -> "HD"
								height >= 720 -> "720p"
								else -> "SD"
							}
							Text(
								text = quality,
								style = MaterialTheme.typography.titleLarge.copy(
									fontSize = 16.sp,
									fontWeight = FontWeight.Bold,
								),
								color = JellyfinColors.Primary,
								modifier = Modifier
									.background(
										color = Color.Black.copy(alpha = 0.6f),
										shape = RoundedCornerShape(4.dp),
									)
									.padding(horizontal = 8.dp, vertical = 2.dp),
							)
						}
					}

					// Audio format indicator (for episodes/movies)
					item.mediaStreams?.firstOrNull { it.type == MediaStreamType.AUDIO }?.let { audioStream ->
						audioStream.codec?.let { codec ->
							Text(
								text = codec.uppercase(),
								style = MaterialTheme.typography.titleLarge.copy(
									fontSize = 14.sp,
									fontWeight = FontWeight.Medium,
								),
								color = Color.White.copy(alpha = 0.6f),
								modifier = Modifier
									.background(
										color = Color.Black.copy(alpha = 0.6f),
										shape = RoundedCornerShape(4.dp),
									)
									.padding(horizontal = 6.dp, vertical = 2.dp),
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
						maxLines = 3,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.padding(bottom = 24.dp, end = 400.dp), // Added more right padding for logo buffer
					)
				}
			}
		}
		
		// Item logo positioned absolutely at top right, aligned with cards
		focusedItem?.let { item ->
			val logoUrl = getItemLogoUrl(item)
			if (logoUrl != null) {
				AsyncImage(
					model = logoUrl,
					contentDescription = "${item.name} logo",
					modifier = Modifier
						.align(Alignment.TopEnd)
						.padding(top = 120.dp, end = 60.dp) // Moved logo up by 20dp, added more right buffer
						.height(110.dp) // Slightly increased height for better visibility
						.widthIn(max = 380.dp), // Increased max width with more buffer space
					contentScale = ContentScale.Fit,
				)
			}
		}
	}
}

/**
 * Cards section positioned in the lower portion of the screen
 */
@Composable
private fun CardsSection(
	items: List<BaseItemDto>,
	layout: ImmersiveListLayout,
	focusedItem: BaseItemDto?,
	onItemClick: (BaseItemDto) -> Unit,
	onItemFocus: (BaseItemDto) -> Unit,
	getItemImageUrl: (BaseItemDto) -> String?,
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier.padding(horizontal = 48.dp, vertical = 16.dp),
	) {
		when (layout) {
			ImmersiveListLayout.HORIZONTAL_CARDS -> {
				HorizontalCardsList(
					items = items,
					focusedItem = focusedItem,
					onItemClick = onItemClick,
					onItemFocus = onItemFocus,
					getItemImageUrl = getItemImageUrl,
					modifier = Modifier.fillMaxSize(),
				)
			}
			ImmersiveListLayout.CAST_ROW -> {
				HorizontalCardsList(
					items = items,
					focusedItem = focusedItem,
					onItemClick = onItemClick,
					onItemFocus = onItemFocus,
					getItemImageUrl = getItemImageUrl,
					modifier = Modifier.fillMaxSize(),
				)
			}
			ImmersiveListLayout.VERTICAL_GRID -> {
				VerticalCardsGrid(
					items = items,
					focusedItem = focusedItem,
					onItemClick = onItemClick,
					onItemFocus = onItemFocus,
					getItemImageUrl = getItemImageUrl,
					modifier = Modifier.fillMaxSize(),
				)
			}
		}
	}
}

/**
 * Horizontal cards layout for immersive list
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HorizontalCardsList(
	items: List<BaseItemDto>,
	focusedItem: BaseItemDto?,
	onItemClick: (BaseItemDto) -> Unit,
	onItemFocus: (BaseItemDto) -> Unit,
	getItemImageUrl: (BaseItemDto) -> String?,
	modifier: Modifier = Modifier,
) {
	val listState = rememberLazyListState()

	LazyRow(
		state = listState,
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
		modifier = modifier,
	) {
		items(items) { item ->
			Box(
				modifier = Modifier.padding(4.dp), // Add padding around each card for focus scaling
			) {
				ImmersiveListCard(
					item = item,
					onClick = { onItemClick(item) },
					onFocus = { onItemFocus(item) },
					imageUrl = getItemImageUrl(item),
					modifier = Modifier.width(300.dp).height(169.dp), // 16:9 aspect ratio for horizontal cards
				)
			}
		}
	}
}

/**
 * Vertical grid layout for immersive list
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VerticalCardsGrid(
	items: List<BaseItemDto>,
	focusedItem: BaseItemDto?,
	onItemClick: (BaseItemDto) -> Unit,
	onItemFocus: (BaseItemDto) -> Unit,
	getItemImageUrl: (BaseItemDto) -> String?,
	modifier: Modifier = Modifier,
) {
	val gridState = rememberLazyGridState()

	LazyVerticalGrid(
		columns = GridCells.Fixed(3), // Reduced from 6 to 3 for horizontal cards
		state = gridState,
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp),
		contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
		modifier = modifier,
	) {
		items(items) { item ->
			Box(
				modifier = Modifier.padding(4.dp), // Add padding around each card for focus scaling
			) {
				ImmersiveListCard(
					item = item,
					onClick = { onItemClick(item) },
					onFocus = { onItemFocus(item) },
					imageUrl = getItemImageUrl(item),
					modifier = Modifier.aspectRatio(16f / 9f), // 16:9 aspect ratio for horizontal cards
				)
			}
		}
	}
}

/**
 * Card component for immersive list
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ImmersiveListCard(
	item: BaseItemDto,
	onClick: () -> Unit,
	onFocus: () -> Unit,
	imageUrl: String?,
	modifier: Modifier = Modifier,
) {
	var isFocused by remember { mutableStateOf(false) }

	androidx.tv.material3.Card(
		onClick = onClick,
		modifier = modifier
			.onFocusChanged { focusState ->
				isFocused = focusState.isFocused
				if (focusState.isFocused) {
					onFocus()
				}
			},
		colors = androidx.tv.material3.CardDefaults.colors(
			containerColor = if (isFocused) JellyfinColors.FocusedContainer else Color.Transparent,
		),
		scale = androidx.tv.material3.CardDefaults.scale(
			focusedScale = 1.05f, // Reduced from 1.1f to 1.05f to prevent cutoff
			pressedScale = 0.98f, // Slightly less aggressive pressed scale
		),
		border = androidx.tv.material3.CardDefaults.border(
			focusedBorder = androidx.tv.material3.Border(
				border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
				shape = RoundedCornerShape(8.dp),
			),
		),
		shape = androidx.tv.material3.CardDefaults.shape(RoundedCornerShape(8.dp)),
	) {
		Box(
			modifier = Modifier.fillMaxSize(),
		) {
			// Background image
			AsyncImage(
				model = imageUrl,
				contentDescription = item.name,
				modifier = Modifier.fillMaxSize(),
				contentScale = ContentScale.Crop,
			)

			// Gradient overlay for text readability
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(
						Brush.verticalGradient(
							colors = listOf(
								Color.Transparent,
								Color.Black.copy(alpha = 0.7f),
							),
							startY = 0.5f,
						),
					),
			)

			// Title overlay
			Column(
				modifier = Modifier
					.align(Alignment.BottomStart)
					.padding(12.dp),
			) {
				Text(
					text = item.name ?: "Unknown Title",
					style = MaterialTheme.typography.bodyMedium.copy(
						fontWeight = FontWeight.SemiBold,
						fontSize = 14.sp,
					),
					color = Color.White,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
				)

				// Additional metadata
				item.productionYear?.let { year ->
					Text(
						text = year.toString(),
						style = MaterialTheme.typography.bodySmall,
						color = Color.White.copy(alpha = 0.8f),
						fontSize = 12.sp,
					)
				}
			}
		}
	}
}

/**
 * Helper data class for immersive list sections
 */
data class ImmersiveListSection(
	val title: String,
	val items: List<BaseItemDto>,
	val layout: ImmersiveListLayout = ImmersiveListLayout.HORIZONTAL_CARDS,
)

/**
 * Multi-section immersive list
 */
@Composable
fun MultiSectionImmersiveList(
	sections: List<ImmersiveListSection>,
	modifier: Modifier = Modifier,
	onItemClick: (BaseItemDto) -> Unit = {},
	onItemFocus: (BaseItemDto) -> Unit = {},
	getItemImageUrl: (BaseItemDto) -> String? = { null },
	getItemBackdropUrl: (BaseItemDto) -> String? = { null },
	getItemLogoUrl: (BaseItemDto) -> String? = { null },
) {
	var globalFocusedItem by remember { mutableStateOf<BaseItemDto?>(null) }

	LaunchedEffect(globalFocusedItem) {
		if (globalFocusedItem != null) {
			delay(150)
			onItemFocus(globalFocusedItem!!)
		}
	}

	Box(modifier = modifier.fillMaxSize()) {
		// Background
		ImmersiveBackground(
			item = globalFocusedItem,
			getBackdropUrl = getItemBackdropUrl,
		)

		// Content
		LazyColumn(
			verticalArrangement = Arrangement.spacedBy(32.dp),
			contentPadding = PaddingValues(bottom = 32.dp),
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
			items(sections) { section ->
				ImmersiveList(
					title = section.title,
					items = section.items,
					layout = section.layout,
					backgroundMode = BackgroundMode.NONE, // Background handled globally
					onItemClick = onItemClick,
					onItemFocus = { item -> globalFocusedItem = item },
					getItemImageUrl = getItemImageUrl,
					getItemBackdropUrl = getItemBackdropUrl,
					getItemLogoUrl = getItemLogoUrl,
					modifier = Modifier
						.fillMaxWidth()
						.height(
							when (section.layout) {
								ImmersiveListLayout.HORIZONTAL_CARDS -> 280.dp
								ImmersiveListLayout.CAST_ROW -> 260.dp
								ImmersiveListLayout.VERTICAL_GRID -> 600.dp
							},
						),
				)
			}
		}
	}
}
