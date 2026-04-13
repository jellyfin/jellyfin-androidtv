package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.ProvideTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.base.popover.Popover
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.backend.PlayerTrack
import org.jellyfin.playback.core.backend.TrackType
import java.util.Locale

@Composable
fun AudioTrackButton(
	playbackManager: PlaybackManager,
) {
	val trackBackend = playbackManager.trackSelection ?: return

	var expanded by remember { mutableStateOf(false) }
	var tracks by remember { mutableStateOf<List<PlayerTrack>?>(null) }

	val availableTracks = remember(expanded) {
		if (expanded || tracks == null) {
			trackBackend.getAvailableTracks(TrackType.AUDIO).also { tracks = it }
		} else tracks!!
	}

	if (availableTracks.size < 2) return

	Box {
		IconButton(
			onClick = {
				tracks = trackBackend.getAvailableTracks(TrackType.AUDIO)
				expanded = true
			},
		) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_select_audio),
				contentDescription = stringResource(R.string.lbl_audio_track),
			)
		}

		TrackSelectionPopover(
			expanded = expanded,
			onDismissRequest = { expanded = false },
			tracks = tracks ?: emptyList(),
			title = stringResource(R.string.lbl_audio_track),
			onTrackSelected = { track ->
				track?.let { trackBackend.selectTrack(TrackType.AUDIO, it.index) }
				expanded = false
			},
		)
	}
}

@Composable
fun SubtitleTrackButton(
	playbackManager: PlaybackManager,
) {
	val trackBackend = playbackManager.trackSelection ?: return

	var expanded by remember { mutableStateOf(false) }
	var tracks by remember { mutableStateOf<List<PlayerTrack>?>(null) }

	val availableTracks = remember(expanded) {
		if (expanded || tracks == null) {
			trackBackend.getAvailableTracks(TrackType.SUBTITLE).also { tracks = it }
		} else tracks!!
	}

	if (availableTracks.isEmpty()) return

	Box {
		IconButton(
			onClick = {
				tracks = trackBackend.getAvailableTracks(TrackType.SUBTITLE)
				expanded = true
			},
		) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_select_subtitle),
				contentDescription = stringResource(R.string.lbl_subtitle_track),
			)
		}

		TrackSelectionPopover(
			expanded = expanded,
			onDismissRequest = { expanded = false },
			tracks = tracks ?: emptyList(),
			title = stringResource(R.string.lbl_subtitle_track),
			showNoneOption = true,
			onTrackSelected = { track ->
				trackBackend.selectTrack(TrackType.SUBTITLE, track?.index ?: -1)
				expanded = false
			},
		)
	}
}

@Composable
private fun TrackSelectionPopover(
	expanded: Boolean,
	onDismissRequest: () -> Unit,
	tracks: List<PlayerTrack>,
	title: String,
	showNoneOption: Boolean = false,
	onTrackSelected: (PlayerTrack?) -> Unit,
) {
	Popover(
		expanded = expanded,
		onDismissRequest = onDismissRequest,
		alignment = Alignment.TopCenter,
		offset = DpOffset(0.dp, (-5).dp),
	) {
		Column(
			modifier = Modifier
				.padding(8.dp)
				.widthIn(min = 180.dp)
		) {
			Text(
				text = title,
				style = JellyfinTheme.typography.listHeader.copy(
					color = JellyfinTheme.colorScheme.listHeader
				),
				modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
			)

			if (showNoneOption) {
				TrackItem(
					label = stringResource(R.string.lbl_none),
					isSelected = tracks.none { it.isSelected },
					onClick = { onTrackSelected(null) },
				)
			}

			tracks.forEach { track ->
				TrackItem(
					label = track.displayLabel,
					isSelected = track.isSelected,
					onClick = { onTrackSelected(track) },
				)
			}
		}
	}
}

@Composable
private fun TrackItem(
	label: String,
	isSelected: Boolean,
	onClick: () -> Unit,
) {
	Button(
		onClick = onClick,
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
		) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_check),
				contentDescription = null,
				tint = if (isSelected) JellyfinTheme.colorScheme.onBackground else JellyfinTheme.colorScheme.onBackground.copy(alpha = 0f),
			)
			ProvideTextStyle(JellyfinTheme.typography.listHeadline) {
				Text(text = label)
			}
		}
	}
}

private val PlayerTrack.displayLabel: String
	get() {
		val languageName = language?.let { code ->
			try {
				Locale.forLanguageTag(code).displayLanguage.takeIf { it.isNotBlank() && it != code }
			} catch (e: Exception) {
				null
			}
		}

		return buildString {
			when {
				!label.isNullOrBlank() -> append(label)
				languageName != null -> append(languageName)
				!language.isNullOrBlank() -> append(language)
				else -> append("Track ${index + 1}")
			}
		}
	}
