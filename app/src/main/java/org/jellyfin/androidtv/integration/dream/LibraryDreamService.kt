package org.jellyfin.androidtv.integration.dream

import android.service.dreams.DreamService
import android.text.format.DateUtils
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.overscan
import org.jellyfin.androidtv.ui.composable.rememberCurrentTime
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageFormat
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.constant.ItemSortBy
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.get
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@Composable
fun DreamHeader(
	showLogo: Boolean,
	showClock: Boolean,
) {
	Row(
		horizontalArrangement = Arrangement.SpaceBetween,
		modifier = Modifier
			.fillMaxWidth()
			.overscan(),
	) {
		// Logo
		AnimatedVisibility(
			visible = showLogo,
			enter = fadeIn(),
			exit = fadeOut(),
			modifier = Modifier.height(41.dp),
		) {
			Image(
				painter = painterResource(R.drawable.app_logo),
				contentDescription = stringResource(R.string.app_name),
			)
		}

		Spacer(
			modifier = Modifier
				.fillMaxWidth(0f)
		)

		// Clock
		AnimatedVisibility(
			visible = showClock,
			enter = fadeIn(),
			exit = fadeOut(),
		) {
			val time = rememberCurrentTime()
			Text(
				text = DateUtils.formatDateTime(LocalContext.current, time, DateUtils.FORMAT_SHOW_TIME),
				style = TextStyle(
					color = Color.White,
					fontSize = 20.sp,
					shadow = Shadow(
						color = Color.Black,
						blurRadius = 2f,
					)
				),
			)
		}
	}
}

@Composable
fun ZoomBox(
	initialValue: Float = 1f,
	targetValue: Float = 2f,
	durationMillis: Int = 1_000,
	content: @Composable BoxScope.() -> Unit,
) {
	val transition = rememberInfiniteTransition()
	val scale by transition.animateFloat(
		initialValue = initialValue,
		targetValue = targetValue,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis, easing = LinearEasing),
			repeatMode = RepeatMode.Reverse
		)
	)

	Box(
		modifier = Modifier.graphicsLayer {
			scaleX = scale
			scaleY = scale
		},
		content = content,
	)
}

@Composable
fun DreamLibraryCarousel(
	currentItem: BaseItemDto?
) {
	val api = get<ApiClient>()

	Box(
		modifier = Modifier.fillMaxSize()
	) {
		Crossfade(
			targetState = currentItem,
			animationSpec = tween(durationMillis = 1_000),
		) { item ->
			if (item != null) {
				// Image
				val tag = item.backdropImageTags?.randomOrNull()
					?: item.imageTags?.get(ImageType.BACKDROP)

				ZoomBox(
					initialValue = 1f,
					targetValue = 1.1f,
					durationMillis = 30_000,
				) {
					AsyncImage(
						url = api.imageApi.getItemImageUrl(
							itemId = item.id,
							imageType = ImageType.BACKDROP,
							tag = tag,
							format = ImageFormat.WEBP,
						),
						scaleType = ImageView.ScaleType.CENTER_CROP,
						modifier = Modifier.fillMaxSize(),
					)
				}

				// Overlay
				Row(
					modifier = Modifier
						.align(Alignment.BottomStart)
						.overscan(),
				) {
					Text(
						text = item.name.orEmpty(),
						style = TextStyle(
							color = Color.White,
							fontSize = 32.sp,
							shadow = Shadow(
								color = Color.Black,
								blurRadius = 2f,
							)
						),
					)
				}
			}
		}
	}
}

/**
 * An Android [DreamService] (screensaver) that shows TV series and movies from all libraries.
 * Use `adb shell am start -n "com.android.systemui/.Somnambulator"` to start after changing the
 * default screensaver in the device settings.
 */
class LibraryDreamService : DreamServiceCompat() {
	private val api: ApiClient by inject()
	private val userPreferences: UserPreferences by inject()

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()

		isInteractive = false
		isFullscreen = true

		setContent {
			var currentItem by remember { mutableStateOf<BaseItemDto?>(null) }

			LaunchedEffect(true) {
				delay(2.seconds)

				while (true) {
					currentItem = getRandomItem()
					delay(30.seconds)
				}
			}
			Box(
				modifier = Modifier.fillMaxSize()
			) {
				// Background
				AnimatedVisibility(
					visible = currentItem == null,
					enter = fadeIn(),
					exit = fadeOut(),
					modifier = Modifier
						.align(Alignment.Center)
						.width(400.dp),
				) {
					Image(
						painter = painterResource(R.drawable.app_logo),
						contentDescription = getString(R.string.app_name),
					)
				}

				// Carousel
				DreamLibraryCarousel(currentItem)

				// Header overlay
				DreamHeader(
					showLogo = currentItem != null,
					showClock = when (userPreferences[UserPreferences.clockBehavior]) {
						ClockBehavior.ALWAYS, ClockBehavior.IN_MENUS -> true
						else -> false
					}
				)
			}

		}
	}

	private suspend fun getRandomItem(): BaseItemDto? = try {
		val response by api.itemsApi.getItemsByUserId(
			includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			sortBy = listOf(ItemSortBy.Random),
			limit = 1,
			imageTypes = listOf(ImageType.BACKDROP)
		)

		response.items?.firstOrNull()
	} catch (err: ApiClientException) {
		Timber.e(err)
		null
	}
}
