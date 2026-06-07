package org.jellyfin.androidtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.compose.koinInject

@Composable
fun HomeHeroSection(
	viewModel: HomeViewModel,
	modifier: Modifier = Modifier,
) {
	val focusedItem by viewModel.focusedItem.collectAsState()
	val api = koinInject<ApiClient>()

	Box(
		modifier = modifier
			.background(Color.Black) // Solid background as fallback
	) {
		// Background image
		focusedItem?.let { item ->
			val backdropUrl = item.baseItem?.itemBackdropImages?.firstOrNull()?.getUrl(api)

			if (backdropUrl != null) {
				AsyncImage(
					model = ImageRequest.Builder(LocalContext.current)
						.data(backdropUrl)
						.crossfade(300)
						.build(),
					contentDescription = null,
					modifier = Modifier.matchParentSize(),
					contentScale = ContentScale.Crop
				)
			}
		}

		// Gradient overlay - strong gradient to make rows visible
		Box(
			modifier = Modifier
				.matchParentSize()
				.background(
					brush = Brush.verticalGradient(
						colors = listOf(
							Color.Black.copy(alpha = 0.3f),
							Color.Black.copy(alpha = 0.6f),
							Color.Black.copy(alpha = 0.95f)
						),
						startY = 0f,
						endY = 2000f
					)
				)
		)

		// Content - positioned at top
		Column(
			modifier = Modifier
				.align(Alignment.TopStart)
				.padding(start = 48.dp, top = 120.dp, end = 48.dp)
				.fillMaxWidth(0.6f)
		) {
			// Show placeholder when no item is focused
			if (focusedItem == null) {
				Text(
					text = "Select an item to see details",
					fontSize = 24.sp,
					color = Color.White.copy(alpha = 0.6f)
				)
			}

			focusedItem?.let { item ->
				// Title
				Text(
					text = item.getFullName(LocalContext.current) ?: "",
					fontSize = 36.sp,
					fontWeight = FontWeight.Bold,
					color = Color.White,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis
				)

				Spacer(modifier = Modifier.height(8.dp))

				// Metadata row
				Row(
					verticalAlignment = Alignment.CenterVertically
				) {
					item.baseItem?.let { baseItem ->
						// Community rating
						baseItem.communityRating?.let { rating ->
							Text(
								text = "★ %.1f".format(rating),
								fontSize = 18.sp,
								color = Color.White.copy(alpha = 0.9f)
							)
							Spacer(modifier = Modifier.width(16.dp))
						}

						// Year
						baseItem.productionYear?.let { year ->
							Text(
								text = year.toString(),
								fontSize = 18.sp,
								color = Color.White.copy(alpha = 0.9f)
							)
							Spacer(modifier = Modifier.width(16.dp))
						}

						// Runtime
						baseItem.runTimeTicks?.let { ticks ->
							val minutes = (ticks / 600000000).toInt()
							val hours = minutes / 60
							val mins = minutes % 60
							val runtimeText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

							Text(
								text = runtimeText,
								fontSize = 18.sp,
								color = Color.White.copy(alpha = 0.9f)
							)
							Spacer(modifier = Modifier.width(16.dp))
						}

						// Content rating
						baseItem.officialRating?.let { rating ->
							Text(
								text = rating,
								fontSize = 16.sp,
								color = Color.White.copy(alpha = 0.8f),
								modifier = Modifier
									.background(Color.White.copy(alpha = 0.2f))
									.padding(horizontal = 8.dp, vertical = 4.dp)
							)
						}
					}
				}

				Spacer(modifier = Modifier.height(12.dp))

				// Description
				item.baseItem?.overview?.let { overview ->
					Text(
						text = overview,
						fontSize = 14.sp,
						color = Color.White.copy(alpha = 0.85f),
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
						lineHeight = 20.sp
					)
				}
			}
		}
	}
}
