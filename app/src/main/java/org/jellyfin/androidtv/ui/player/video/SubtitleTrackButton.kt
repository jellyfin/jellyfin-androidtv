package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.base.popover.Popover
import org.jellyfin.androidtv.ui.base.popover.PopoverMenu
import org.jellyfin.androidtv.ui.base.popover.PopoverMenuCheckboxItem
import org.jellyfin.playback.core.PlaybackManager

@Composable
fun SubtitleTrackButton(
	playbackManager: PlaybackManager,
) {
	val subtitleTracks by playbackManager.backend.subtitleTrackSelector.tracks.collectAsState()
	var expanded by remember { mutableStateOf(false) }

	Box {
		IconButton(
			enabled = subtitleTracks.isNotEmpty(),
			onClick = { expanded = true },
		) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_select_subtitle),
				contentDescription = stringResource(R.string.lbl_subtitle_track),
			)
		}

		Popover(
			expanded = expanded,
			onDismissRequest = { expanded = false },
			alignment = Alignment.TopCenter,
		) {
			PopoverMenu {
				PopoverMenuCheckboxItem(
					selected = subtitleTracks.none { it.selected },
					onClick = {
						playbackManager.backend.subtitleTrackSelector.select(null)
						expanded = false
					},
				) {
					Text(stringResource(R.string.lbl_none))
				}

				subtitleTracks.forEach { track ->
					val label = listOfNotNull(
						track.label?.takeIf(String::isNotBlank),
						track.language?.takeIf { language -> language.isNotBlank() && language != track.label },
					).joinToString(" - ").ifBlank {
						stringResource(R.string.lbl_subtitle_track)
					}

					PopoverMenuCheckboxItem(
						selected = track.selected,
						onClick = {
							playbackManager.backend.subtitleTrackSelector.select(track.id)
							expanded = false
						},
					) {
						Text(label)
					}
				}
			}
		}
	}
}
