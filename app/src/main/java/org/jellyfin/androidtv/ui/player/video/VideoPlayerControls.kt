package org.jellyfin.androidtv.ui.player.video

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.base.popover.Popover
import org.jellyfin.androidtv.ui.composable.rememberPlayerPositionInfo
import org.jellyfin.androidtv.ui.player.base.PlayerSeekbar
import org.jellyfin.androidtv.ui.syncplay.SyncPlayViewModel
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayClient
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayRequest
import org.jellyfin.sdk.model.api.SyncPlayUserAccessType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private const val TICKS_PER_MILLISECOND = 10_000L

@Composable
fun VideoPlayerControls(
	playbackManager: PlaybackManager = koinInject(),
	onPlaybackInfoClick: () -> Unit = {},
) {
	val playState by playbackManager.state.playState.collectAsState()
	val syncPlay = koinInject<SyncPlayClient>()
	val userRepository = koinInject<UserRepository>()
	val syncPlayViewModel = koinActivityViewModel<SyncPlayViewModel>()
	val currentUser by userRepository.currentUser.collectAsState()
	val syncState by syncPlay.state.collectAsState()
	val coroutineScope = rememberCoroutineScope()
	val groupMember = syncState.group != null
	val following = syncState.following
	val occurrence = syncState.queue?.let { it.playlist.getOrNull(it.playingItemIndex)?.playlistItemId }
	val syncPlayAvailable = currentUser?.policy?.syncPlayAccess != null &&
		currentUser?.policy?.syncPlayAccess != SyncPlayUserAccessType.NONE

	Column(
		verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			modifier = Modifier
				.focusRestorer()
				.focusGroup()
		) {
			PlayPauseButton(playbackManager, playState, groupMember, following, syncPlay)
			RewindButton(playbackManager, groupMember, following, syncPlay)
			FastForwardButton(playbackManager, groupMember, following, syncPlay)

			Spacer(Modifier.weight(1f))

			if (syncPlayAvailable) SyncPlayButton(syncPlayViewModel::show)

			PlaybackInfoButton(onClick = onPlaybackInfoClick)

			MoreOptionsButton {
				PreviousEntryButton(playbackManager, groupMember, following, occurrence, syncPlay)
				NextEntryButton(playbackManager, groupMember, following, occurrence, syncPlay)
			}
		}

		PlayerSeekbar(
			playbackManager = playbackManager,
			modifier = Modifier
				.fillMaxWidth()
				.height(4.dp),
			enabled = !groupMember || following,
			onSeek = if (!groupMember) null else { position ->
				coroutineScope.launch { syncPlay.request(SyncPlayRequest.Seek(position.inWholeMilliseconds * TICKS_PER_MILLISECOND)) }
			},
		)

		Row(
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			modifier = Modifier
				.focusRestorer()
				.focusGroup()
		) {
			Spacer(Modifier.weight(1f))
			PositionText(playbackManager)
		}
	}
}

@Composable
private fun SyncPlayButton(onClick: () -> Unit) = IconButton(onClick = onClick) {
	Icon(
		imageVector = ImageVector.vectorResource(R.drawable.ic_users),
		contentDescription = stringResource(R.string.syncplay),
	)
}

@Composable
private fun PlayPauseButton(
	playbackManager: PlaybackManager,
	playState: PlayState,
	groupMember: Boolean,
	following: Boolean,
	syncPlay: SyncPlayClient,
) {
	val focusRequester = remember { FocusRequester() }
	val coroutineScope = rememberCoroutineScope()
	IconButton(
		onClick = {
			if (groupMember) {
				coroutineScope.launch {
					if (!following) syncPlay.resume()
					else syncPlay.request(if (playState == PlayState.PLAYING) SyncPlayRequest.Pause else SyncPlayRequest.Unpause)
				}
				return@IconButton
			}
			when (playState) {
				PlayState.STOPPED,
				PlayState.ERROR -> playbackManager.state.play()

				PlayState.PLAYING -> playbackManager.state.pause()
				PlayState.PAUSED -> playbackManager.state.unpause()
			}
		},
		modifier = Modifier
			.focusRequester(focusRequester)
			.onVisibilityChanged {
				focusRequester.requestFocus()
			}
	) {
		AnimatedContent(playState) { playState ->
			when (playState) {
				PlayState.PLAYING -> {
					Icon(
						imageVector = ImageVector.vectorResource(R.drawable.ic_pause),
						contentDescription = stringResource(R.string.lbl_pause),
					)
				}

				PlayState.STOPPED,
				PlayState.PAUSED,
				PlayState.ERROR -> {
					Icon(
						imageVector = ImageVector.vectorResource(R.drawable.ic_play),
						contentDescription = stringResource(R.string.lbl_play),
					)
				}
			}
		}
	}
}

@Composable
private fun RewindButton(
	playbackManager: PlaybackManager,
	groupMember: Boolean,
	following: Boolean,
	syncPlay: SyncPlayClient,
) {
	val coroutineScope = rememberCoroutineScope()
	IconButton(
		enabled = !groupMember || following,
		onClick = {
			if (groupMember) {
				val target = playbackManager.state.positionInfo.active - playbackManager.options.defaultRewindAmount()
				coroutineScope.launch {
					syncPlay.request(SyncPlayRequest.Seek(target.inWholeMilliseconds.coerceAtLeast(0) * TICKS_PER_MILLISECOND))
				}
			} else playbackManager.state.rewind()
		},
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_rewind),
			contentDescription = stringResource(R.string.rewind),
		)
	}
}

@Composable
private fun FastForwardButton(
	playbackManager: PlaybackManager,
	groupMember: Boolean,
	following: Boolean,
	syncPlay: SyncPlayClient,
) {
	val coroutineScope = rememberCoroutineScope()
	IconButton(
		enabled = !groupMember || following,
		onClick = {
			if (groupMember) {
				val target = playbackManager.state.positionInfo.active + playbackManager.options.defaultFastForwardAmount()
				coroutineScope.launch {
					syncPlay.request(SyncPlayRequest.Seek(target.inWholeMilliseconds * TICKS_PER_MILLISECOND))
				}
			} else playbackManager.state.fastForward()
		},
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_fast_forward),
			contentDescription = stringResource(R.string.fast_forward),
		)
	}
}

@Composable
private fun PreviousEntryButton(
	playbackManager: PlaybackManager,
	groupMember: Boolean,
	following: Boolean,
	occurrence: java.util.UUID?,
	syncPlay: SyncPlayClient,
) {
	val entryIndex by playbackManager.queue.entryIndex.collectAsState()
	val coroutineScope = rememberCoroutineScope()

	IconButton(
		enabled = (following && occurrence != null) || (!groupMember && entryIndex > 0),
		onClick = {
			coroutineScope.launch {
				if (groupMember) syncPlay.request(SyncPlayRequest.Previous(requireNotNull(occurrence)))
				else playbackManager.queue.previous()
			}
		},
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_previous),
			contentDescription = stringResource(R.string.lbl_prev_item),
		)
	}
}

@Composable
private fun NextEntryButton(
	playbackManager: PlaybackManager,
	groupMember: Boolean,
	following: Boolean,
	occurrence: java.util.UUID?,
	syncPlay: SyncPlayClient,
) {
	val entryIndex by playbackManager.queue.entryIndex.collectAsState()
	val coroutineScope = rememberCoroutineScope()

	IconButton(
		enabled = (following && occurrence != null) || (!groupMember && entryIndex < playbackManager.queue.estimatedSize - 1),
		onClick = {
			coroutineScope.launch {
				if (groupMember) syncPlay.request(SyncPlayRequest.Next(requireNotNull(occurrence)))
				else playbackManager.queue.next()
			}
		},
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_next),
			contentDescription = stringResource(R.string.lbl_next_item),
		)
	}
}

private fun Duration.formatted(includeHours: Boolean): String {
	val totalSeconds = toInt(DurationUnit.SECONDS)
	val hours = totalSeconds / 3600
	val minutes = (totalSeconds % 3600) / 60
	val seconds = totalSeconds % 60

	return if (includeHours) "%02d:%02d:%02d".format(hours, minutes, seconds)
	else "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun PositionText(
	playbackManager: PlaybackManager,
) {
	val positionInfo by rememberPlayerPositionInfo(playbackManager, precision = 1.seconds)
	if (positionInfo.duration == Duration.ZERO) return

	val text by remember {
		derivedStateOf {
			val includeHours = positionInfo.duration.inWholeMinutes >= 60
			val activeFormatted = positionInfo.active.formatted(includeHours)
			val durationFormatted = positionInfo.duration.formatted(includeHours)

			"$activeFormatted / $durationFormatted"
		}
	}

	Text(
		text = text,
		style = LocalTextStyle.current.copy(color = Color.White)
	)
}

@Composable
private fun MoreOptionsButton(
	content: @Composable () -> Unit,
) = Box {
	var expanded by remember { mutableStateOf(false) }
	IconButton(
		onClick = { expanded = true },
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_more),
			contentDescription = stringResource(R.string.lbl_other_options),
		)
	}

	Popover(
		expanded = expanded,
		onDismissRequest = { expanded = false },
		alignment = Alignment.TopCenter,
		offset = DpOffset(0.dp, (-5).dp)
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			modifier = Modifier
				.padding(4.dp)
		) {
			content()
		}
	}
}

@Composable
fun PlaybackInfoButton(
	onClick: () -> Unit,
) = IconButton(
	onClick = onClick,
) {
	Icon(
		imageVector = ImageVector.vectorResource(R.drawable.ic_info),
		contentDescription = stringResource(R.string.playback_info),
	)
}
