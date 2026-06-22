package org.jellyfin.androidtv.ui.player.photo

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration

@Composable
fun PhotoPlayerControls() {
	val viewModel = koinViewModel<PhotoPlayerViewModel>()
	val presentationActive by viewModel.presentationActive.collectAsState()
	val presentationDelay by viewModel.presentationDelay.collectAsState()
	val shuffleActive by viewModel.shuffleActive.collectAsState()

	Row(
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		modifier = Modifier
			.focusRestorer()
			.focusGroup()
	) {
		ShuffleButton(
			shuffleActive = shuffleActive,
			onClick = { viewModel.toggleShuffle() },
		)

		PreviousButton(
			onClick = { viewModel.showPrevious() },
		)

		PlayPauseButton(
			presentationActive = presentationActive,
			onSetPresentationActive = { presentationActive ->
				if (presentationActive) viewModel.startPresentation()
				else viewModel.stopPresentation()
			}
		)

		NextButton(
			onClick = { viewModel.showNext() },
		)

		IntervalButton(
			presentationDelay = presentationDelay,
			onClick = { viewModel.cycleInterval() }
		)
	}
}

@Composable
private fun PreviousButton(
	onClick: () -> Unit,
) {
	IconButton(
		onClick = onClick,
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_previous),
			contentDescription = stringResource(R.string.lbl_prev_item),
		)
	}
}

@Composable
private fun NextButton(
	onClick: () -> Unit,
) {
	IconButton(
		onClick = onClick,
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_next),
			contentDescription = stringResource(R.string.lbl_next_item),
		)
	}
}

@Composable
private fun PlayPauseButton(
	presentationActive: Boolean,
	onSetPresentationActive: (presentationActive: Boolean) -> Unit,
) {
	val focusRequester = remember { FocusRequester() }
	IconButton(
		onClick = {
			onSetPresentationActive(!presentationActive)
		},
		modifier = Modifier
			.focusRequester(focusRequester)
			.onVisibilityChanged {
				focusRequester.requestFocus()
			}
	) {
		AnimatedContent(presentationActive) { presentationActive ->
			if (presentationActive) {
				Icon(
					imageVector = ImageVector.vectorResource(R.drawable.ic_pause),
					contentDescription = stringResource(R.string.lbl_pause),
				)
			} else {
				Icon(
					imageVector = ImageVector.vectorResource(R.drawable.ic_play),
					contentDescription = stringResource(R.string.lbl_play),
				)
			}
		}
	}
}

@Composable
private fun ShuffleButton(
	shuffleActive: Boolean,
	onClick: () -> Unit,
) {
	val tintColor = if (shuffleActive) JellyfinTheme.colorScheme.buttonActive else JellyfinTheme.colorScheme.onBackground
	IconButton(
		onClick = onClick,
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_shuffle),
			contentDescription = stringResource(R.string.lbl_shuffle),
			tint = tintColor
		)
	}
}

@Composable
private fun IntervalButton(
	presentationDelay: Duration,
	onClick: () -> Unit,
) {
	Button(
		onClick = onClick,
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(4.dp),
			verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
		) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_time),
				contentDescription = stringResource(R.string.lbl_change_interval),
			)
			Text(
				text = "${presentationDelay.inWholeSeconds}s"
			)
		}
	}
}
