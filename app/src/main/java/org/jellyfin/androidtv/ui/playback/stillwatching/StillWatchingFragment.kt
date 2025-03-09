package org.jellyfin.androidtv.ui.playback.stillwatching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.background.AppBackground
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.button.ProgressButton
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.modifier.overscan
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.TimeUtils.MILLIS_PER_SEC
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.UUID

const val TIMEOUT_IN_MS = 10 * MILLIS_PER_SEC

@Composable
fun StillWatchingScreen(
	itemId: UUID,
) {
	val navigationRepository = koinInject<NavigationRepository>()
	val backgroundService = koinInject<BackgroundService>()
	val viewModel = koinViewModel<StillWatchingViewModel>()

	val state by viewModel.state.collectAsState()

	LaunchedEffect(itemId) {
		viewModel.setItemId(itemId)
	}

	val item by viewModel.item.collectAsState()
	if (item == null) return

	LaunchedEffect(item?.baseItem) {
		backgroundService.setBackground(item?.baseItem)
	}

	LaunchedEffect(state) {
		when (state) {
			// Open next item
			StillWatchingState.STILL_WATCHING -> navigationRepository.navigate(Destinations.videoPlayer(0), true)
			// Close activity
			StillWatchingState.CLOSE -> navigationRepository.goBack()
			// Unknown state
			else -> Unit
		}
	}

	val focusRequester = remember { FocusRequester() }

	Box {
		AppBackground()

		// Logo
		item?.logo?.let { logo ->
			AsyncImage(
				modifier = Modifier
					.align(Alignment.TopStart)
					.overscan()
					.height(75.dp),
				url = logo.url,
				blurHash = logo.blurHash,
				aspectRatio = logo.aspectRatio,
			)
		}

		// Overlay
		StillWatchingOverlay(
			modifier = Modifier
				.align(Alignment.BottomCenter)
				.focusRequester(focusRequester),
			item = requireNotNull(item),
			onConfirm = { viewModel.stillWatching() },
			onCancel = { viewModel.close() },
		)
	}

	LaunchedEffect(focusRequester) {
		focusRequester.requestFocus()
	}
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StillWatchingOverlay(
	modifier: Modifier = Modifier,
	item: StillWatchingItemData,
	onConfirm: () -> Unit,
	onCancel: () -> Unit,
) = ProvideTextStyle(JellyfinTheme.typography.default.copy(color = Color.White)) {
	val endWatchingTimer = remember { Animatable(0f) }
	LaunchedEffect(item) {
		endWatchingTimer.animateTo(
			targetValue = 1f,
			animationSpec = tween(
				durationMillis = TIMEOUT_IN_MS,
				easing = LinearEasing,
			),
		)
		onCancel()
	}

	val focusRequester = remember { FocusRequester() }
	Row(
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		modifier = modifier
			.overscan()
			.fillMaxWidth()
			.focusRestorer { focusRequester }
	) {
		Column(
			modifier = Modifier
				.align(Alignment.Bottom),
			horizontalAlignment = Alignment.Start
		) {
			Text(
				text = stringResource(R.string.lbl_next_up),
				style = LocalTextStyle.current.copy(fontSize = 34.sp),
			)
			Spacer(Modifier.height(4.dp))

			Text(
				text = item.title,
				style = LocalTextStyle.current.copy(fontSize = 16.sp),
				overflow = TextOverflow.Ellipsis,
				maxLines = 1,
			)

			item.thumbnail?.let { thumbnail ->
				Spacer(Modifier.height(16.dp))

				AsyncImage(
					modifier = Modifier
						.align(Alignment.CenterHorizontally)
						.height(145.dp)
						.aspectRatio(thumbnail.aspectRatio.toFloat())
						.clip(JellyfinTheme.shapes.extraSmall),
					url = thumbnail.url,
					blurHash = thumbnail.blurHash,
					aspectRatio = thumbnail.aspectRatio,
				)
			}
		}

		Spacer(
			Modifier
				.weight(1f)
		)

		Column(
			modifier = Modifier
				.align(Alignment.Bottom),
			horizontalAlignment = Alignment.End
		) {
			Text(
				text = stringResource(R.string.still_watching_label),
				style = LocalTextStyle.current.copy(fontSize = 34.sp),
			)

			Spacer(Modifier.height(16.dp))

			Row(
				modifier = Modifier
					.focusGroup()
					.focusRestorer { focusRequester }
			) {
				val coroutineScope = rememberCoroutineScope()
				ProgressButton(
					progress = endWatchingTimer.value,
					onClick = onCancel,
					modifier = Modifier
						.focusRequester(focusRequester)
						.onFocusChanged { state ->
							// Cancel timer if focus is moved away from the button
							if (!state.isFocused) {
								coroutineScope.launch {
									endWatchingTimer.snapTo(0f)
								}
							}
						},
				) {
					Text(stringResource(R.string.lbl_no))
				}

				Spacer(Modifier.width(8.dp))

				Button(onClick = onConfirm) {
					Text(stringResource(R.string.lbl_yes))
				}
			}
		}
	}
}

class StillWatchingFragment : Fragment() {
	companion object {
		const val ARGUMENT_ITEM_ID = "item_id"
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	) = content {
		val id = remember(arguments) { arguments?.getString(ARGUMENT_ITEM_ID)?.toUUIDOrNull() }
		if (id != null) {
			StillWatchingScreen(
				itemId = id,
			)
		}
	}
}
